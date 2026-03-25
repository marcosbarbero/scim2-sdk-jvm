package com.marcosbarbero.scim2.server.adapter.http

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
import com.marcosbarbero.scim2.core.event.BulkOperationCompletedEvent
import com.marcosbarbero.scim2.core.event.NoOpEventPublisher
import com.marcosbarbero.scim2.core.event.ResourceCreatedEvent
import com.marcosbarbero.scim2.core.event.ResourceDeletedEvent
import com.marcosbarbero.scim2.core.event.ResourcePatchedEvent
import com.marcosbarbero.scim2.core.event.ResourceReplacedEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.core.observability.NoOpScimMetrics
import com.marcosbarbero.scim2.core.observability.NoOpScimTracer
import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.core.observability.ScimTracer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.engine.ETagEngine
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import com.marcosbarbero.scim2.server.interceptor.ScimInterceptor
import com.marcosbarbero.scim2.server.port.AuthorizationEvaluator
import com.marcosbarbero.scim2.server.port.BulkHandler
import com.marcosbarbero.scim2.server.port.IdentityResolver
import com.marcosbarbero.scim2.server.port.MeHandler
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Duration
import java.time.Instant

class ScimEndpointDispatcher(
    private val handlers: List<ResourceHandler<*>>,
    private val bulkHandler: BulkHandler?,
    private val meHandler: MeHandler<*>?,
    private val discoveryService: DiscoveryService,
    private val config: ScimServerConfig,
    private val serializer: ScimSerializer,
    private val etagEngine: ETagEngine = ETagEngine(),
    private val interceptors: List<ScimInterceptor> = emptyList(),
    private val identityResolver: IdentityResolver? = null,
    private val authorizationEvaluator: AuthorizationEvaluator? = null,
    private val eventPublisher: ScimEventPublisher = NoOpEventPublisher,
    private val metrics: ScimMetrics = NoOpScimMetrics,
    private val tracer: ScimTracer = NoOpScimTracer
) {

    private val logger = LoggerFactory.getLogger(ScimEndpointDispatcher::class.java)
    private val sortedInterceptors = interceptors.sortedBy { it.order }

    fun dispatch(request: ScimHttpRequest): ScimHttpResponse {
        val context = buildContext(request)
        val relativePath = request.path.removePrefix(config.basePath)
        val endpoint = resolveEndpoint(relativePath)
        val method = request.method.name
        val correlationId = tracer.currentCorrelationId()

        MDC.put("scim.correlationId", correlationId ?: "")
        MDC.put("scim.operation", "${request.method} $relativePath")
        MDC.put("scim.resourceType", resolveResourceType(relativePath))

        metrics.incrementActiveRequests(endpoint)
        val start = Instant.now()

        try {
            var processedRequest = request
            for (interceptor in sortedInterceptors) {
                processedRequest = interceptor.preHandle(processedRequest, context)
            }

            val response = try {
                tracer.trace(
                    "scim.${method.lowercase()}.$endpoint",
                    mapOf("endpoint" to endpoint, "method" to method)
                ) {
                    route(processedRequest, context)
                }
            } catch (e: ScimException) {
                val processedException = sortedInterceptors.fold(e) { ex, interceptor ->
                    interceptor.onError(processedRequest, ex, context)
                }
                val errorBody = serializer.serialize(processedException.toScimError())
                ScimHttpResponse.error(processedException.status, errorBody)
            } catch (e: Exception) {
                logger.error("Unexpected error processing request: {} {}", request.method, request.path, e)
                val errorBody = serializer.serialize(
                    com.marcosbarbero.scim2.core.domain.model.error.ScimError(
                        status = "500",
                        detail = "Internal server error"
                    )
                )
                ScimHttpResponse.error(500, errorBody)
            }

            val duration = Duration.between(start, Instant.now())
            metrics.recordOperation(endpoint, method, response.status, duration)

            var processedResponse = response
            for (interceptor in sortedInterceptors) {
                processedResponse = interceptor.postHandle(processedRequest, processedResponse, context)
            }

            return processedResponse
        } finally {
            metrics.decrementActiveRequests(endpoint)
            MDC.remove("scim.correlationId")
            MDC.remove("scim.operation")
            MDC.remove("scim.resourceType")
        }
    }

    private fun resolveEndpoint(relativePath: String): String {
        val handler = findHandler(relativePath)
        if (handler != null) return handler.endpoint
        val lower = relativePath.lowercase()
        return when {
            lower.startsWith("/serviceproviderconfig") -> "/ServiceProviderConfig"
            lower.startsWith("/schemas") -> "/Schemas"
            lower.startsWith("/resourcetypes") -> "/ResourceTypes"
            lower == "/bulk" -> "/Bulk"
            lower == "/me" -> "/Me"
            else -> relativePath
        }
    }

    private fun resolveResourceType(relativePath: String): String {
        val handler = findHandler(relativePath)
        if (handler != null) return handler.resourceType.simpleName ?: "Unknown"
        val lower = relativePath.lowercase()
        return when {
            lower == "/bulk" -> "Bulk"
            lower == "/me" -> "Me"
            else -> "Unknown"
        }
    }

    private fun route(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpResponse {
        val relativePath = request.path.removePrefix(config.basePath)
        val lowerPath = relativePath.lowercase()

        return when {
            // Discovery endpoints
            lowerPath == "/serviceproviderconfig" && request.method == HttpMethod.GET ->
                ok(discoveryService.getServiceProviderConfig())

            lowerPath == "/schemas" && request.method == HttpMethod.GET ->
                ok(discoveryService.getSchemas())

            lowerPath.startsWith("/schemas/") && request.method == HttpMethod.GET -> {
                val schemaId = relativePath.substring("/Schemas/".length) // preserve original case for schema ID lookup
                ok(discoveryService.getSchema(schemaId))
            }

            lowerPath == "/resourcetypes" && request.method == HttpMethod.GET ->
                ok(discoveryService.getResourceTypes())

            lowerPath.startsWith("/resourcetypes/") && request.method == HttpMethod.GET -> {
                val name = relativePath.substring("/ResourceTypes/".length) // preserve original case
                ok(discoveryService.getResourceType(name))
            }

            // Bulk endpoint
            lowerPath == "/bulk" && request.method == HttpMethod.POST -> {
                val handler = bulkHandler
                    ?: throw ScimException(status = 501, detail = "Bulk operations not supported")
                requireAuthorization { it.canBulk(context) }
                val bulkRequest = deserializeBody<com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest>(request)
                val result = handler.processBulk(bulkRequest, context)
                val failureCount = result.operations.count { !it.status.startsWith("2") }
                eventPublisher.publish(
                    BulkOperationCompletedEvent(
                        correlationId = tracer.currentCorrelationId(),
                        operationCount = result.operations.size,
                        failureCount = failureCount
                    )
                )
                ok(result)
            }

            // Me endpoint
            lowerPath == "/me" -> handleMe(request, context)

            // Resource endpoints
            else -> handleResource(relativePath, request, context)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleResource(
        relativePath: String,
        request: ScimHttpRequest,
        context: ScimRequestContext
    ): ScimHttpResponse {
        val handler = findHandler(relativePath) as? ResourceHandler<ScimResource>
            ?: throw ResourceNotFoundException("No handler found for path: $relativePath")

        // Find where the endpoint matches (case-insensitive) and extract the rest
        val endpointLength = handler.endpoint.length
        val pathAfterEndpoint = relativePath.substring(endpointLength)
        val resourceTypeName = handler.resourceType.simpleName ?: "Unknown"

        // POST /{endpoint}/.search
        if (request.method == HttpMethod.POST && pathAfterEndpoint.equals("/.search", ignoreCase = true)) {
            requireAuthorization { it.canSearch(handler.endpoint, context) }
            val searchRequest = deserializeBody<SearchRequest>(request)
            val result = handler.search(searchRequest, context)
            return ok(result)
        }

        // Extract resource ID if present
        val resourceId = pathAfterEndpoint.removePrefix("/").takeIf { it.isNotEmpty() }

        return when (request.method) {
            HttpMethod.POST -> {
                if (resourceId != null) {
                    throw ScimException(status = 405, detail = "POST not allowed on resource instance")
                }
                requireAuthorization { it.canCreate(handler.endpoint, context) }
                val resource = deserializeBody(request, handler.resourceType)
                val created = handler.create(resource, context)
                val location = "${config.basePath}${handler.endpoint}/${created.id}"
                eventPublisher.publish(
                    ResourceCreatedEvent(
                        resourceType = resourceTypeName,
                        resourceId = created.id ?: "",
                        correlationId = tracer.currentCorrelationId()
                    )
                )
                ScimHttpResponse.created(serializer.serialize(created), location)
            }

            HttpMethod.GET -> {
                if (resourceId == null || resourceId.isEmpty()) {
                    // Search via GET
                    requireAuthorization { it.canSearch(handler.endpoint, context) }
                    val searchRequest = buildSearchFromQuery(request)
                    val result = handler.search(searchRequest, context)
                    ok(result)
                } else {
                    requireAuthorization { it.canRead(handler.endpoint, resourceId, context) }
                    val result = handler.get(ResourceId(resourceId), context)
                    val etag = etagEngine.generateETag(result)
                    val ifNoneMatch = etagEngine.extractIfNoneMatch(request)
                    if (ifNoneMatch != null && etag == ifNoneMatch) {
                        ScimHttpResponse(status = 304)
                    } else {
                        ScimHttpResponse.ok(
                            serializer.serialize(result),
                            headers = if (config.etagEnabled) mapOf("ETag" to etag.value) else emptyMap()
                        )
                    }
                }
            }

            HttpMethod.PUT -> {
                requireNotNull(resourceId) { "PUT requires a resource ID" }
                requireAuthorization { it.canUpdate(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                val resource = deserializeBody(request, handler.resourceType)
                val result = handler.replace(ResourceId(resourceId), resource, ifMatch, context)
                eventPublisher.publish(
                    ResourceReplacedEvent(
                        resourceType = resourceTypeName,
                        resourceId = resourceId,
                        correlationId = tracer.currentCorrelationId()
                    )
                )
                ok(result)
            }

            HttpMethod.PATCH -> {
                requireNotNull(resourceId) { "PATCH requires a resource ID" }
                requireAuthorization { it.canUpdate(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                val patchRequest = deserializeBody<PatchRequest>(request)
                val result = handler.patch(ResourceId(resourceId), patchRequest, ifMatch, context)
                eventPublisher.publish(
                    ResourcePatchedEvent(
                        resourceType = resourceTypeName,
                        resourceId = resourceId,
                        correlationId = tracer.currentCorrelationId(),
                        operationCount = patchRequest.operations.size
                    )
                )
                ok(result)
            }

            HttpMethod.DELETE -> {
                requireNotNull(resourceId) { "DELETE requires a resource ID" }
                requireAuthorization { it.canDelete(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                handler.delete(ResourceId(resourceId), ifMatch, context)
                eventPublisher.publish(
                    ResourceDeletedEvent(
                        resourceType = resourceTypeName,
                        resourceId = resourceId,
                        correlationId = tracer.currentCorrelationId()
                    )
                )
                ScimHttpResponse.noContent()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleMe(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpResponse {
        val handler = meHandler as? MeHandler<ScimResource>
            ?: throw ScimException(status = 501, detail = "/Me endpoint not supported")

        return when (request.method) {
            HttpMethod.GET -> {
                val result = handler.getMe(context)
                ok(result)
            }

            HttpMethod.PUT -> {
                val ifMatch = etagEngine.extractIfMatch(request)
                val resource = deserializeBody<ScimResource>(request)
                val result = handler.replaceMe(context, resource, ifMatch)
                ok(result)
            }

            HttpMethod.PATCH -> {
                val ifMatch = etagEngine.extractIfMatch(request)
                val patchRequest = deserializeBody<PatchRequest>(request)
                val result = handler.patchMe(context, patchRequest, ifMatch)
                ok(result)
            }

            HttpMethod.DELETE -> {
                val ifMatch = etagEngine.extractIfMatch(request)
                handler.deleteMe(context, ifMatch)
                ScimHttpResponse.noContent()
            }

            HttpMethod.POST -> throw ScimException(status = 405, detail = "POST not allowed on /Me")
        }
    }

    private fun findHandler(relativePath: String): ResourceHandler<*>? =
        handlers.firstOrNull { relativePath.lowercase().startsWith(it.endpoint.lowercase()) }

    private fun buildContext(request: ScimHttpRequest): ScimRequestContext {
        val requestedAttrs = request.queryParam("attributes")
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()
        val excludedAttrs = request.queryParam("excludedAttributes")
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()

        return if (identityResolver != null) {
            val resolved = identityResolver.resolve(request)
            resolved.copy(
                requestedAttributes = requestedAttrs,
                excludedAttributes = excludedAttrs
            )
        } else {
            ScimRequestContext(
                requestedAttributes = requestedAttrs,
                excludedAttributes = excludedAttrs
            )
        }
    }

    private fun buildSearchFromQuery(request: ScimHttpRequest): SearchRequest =
        SearchRequest(
            filter = request.queryParam("filter"),
            sortBy = request.queryParam("sortBy"),
            sortOrder = request.queryParam("sortOrder")?.let {
                com.marcosbarbero.scim2.core.domain.model.search.SortOrder.entries
                    .firstOrNull { order -> order.value.equals(it, ignoreCase = true) }
            },
            startIndex = request.queryParam("startIndex")?.toIntOrNull(),
            count = request.queryParam("count")?.toIntOrNull(),
            attributes = request.queryParam("attributes")
                ?.split(",")
                ?.map { it.trim() }
                ?.takeIf { it.isNotEmpty() },
            excludedAttributes = request.queryParam("excludedAttributes")
                ?.split(",")
                ?.map { it.trim() }
                ?.takeIf { it.isNotEmpty() }
        )

    private fun <T : Any> ok(body: T): ScimHttpResponse =
        ScimHttpResponse.ok(serializer.serialize(body))

    private inline fun <reified T : Any> deserializeBody(request: ScimHttpRequest): T {
        val body = request.body
            ?: throw com.marcosbarbero.scim2.core.domain.model.error.InvalidSyntaxException("Request body is required")
        return serializer.deserialize(body, T::class)
    }

    private fun <T : Any> deserializeBody(request: ScimHttpRequest, type: Class<T>): T {
        val body = request.body
            ?: throw com.marcosbarbero.scim2.core.domain.model.error.InvalidSyntaxException("Request body is required")
        return serializer.deserialize(body, type.kotlin)
    }

    private inline fun requireAuthorization(check: (AuthorizationEvaluator) -> Boolean) {
        val evaluator = authorizationEvaluator ?: return
        if (!check(evaluator)) {
            throw ScimException(status = 403, detail = "Forbidden")
        }
    }
}
