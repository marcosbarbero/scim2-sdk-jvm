package com.marcosbarbero.scim2.server.adapter.http

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
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
    private val authorizationEvaluator: AuthorizationEvaluator? = null
) {

    private val logger = LoggerFactory.getLogger(ScimEndpointDispatcher::class.java)
    private val sortedInterceptors = interceptors.sortedBy { it.order }

    fun dispatch(request: ScimHttpRequest): ScimHttpResponse {
        val context = buildContext(request)

        var processedRequest = request
        for (interceptor in sortedInterceptors) {
            processedRequest = interceptor.preHandle(processedRequest, context)
        }

        val response = try {
            route(processedRequest, context)
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

        var processedResponse = response
        for (interceptor in sortedInterceptors) {
            processedResponse = interceptor.postHandle(processedRequest, processedResponse, context)
        }

        return processedResponse
    }

    private fun route(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpResponse {
        val relativePath = request.path.removePrefix(config.basePath)

        return when {
            // Discovery endpoints
            relativePath == "/ServiceProviderConfig" && request.method == HttpMethod.GET ->
                ok(discoveryService.getServiceProviderConfig())

            relativePath == "/Schemas" && request.method == HttpMethod.GET ->
                ok(discoveryService.getSchemas())

            relativePath.startsWith("/Schemas/") && request.method == HttpMethod.GET -> {
                val schemaId = relativePath.removePrefix("/Schemas/")
                ok(discoveryService.getSchema(schemaId))
            }

            relativePath == "/ResourceTypes" && request.method == HttpMethod.GET ->
                ok(discoveryService.getResourceTypes())

            relativePath.startsWith("/ResourceTypes/") && request.method == HttpMethod.GET -> {
                val name = relativePath.removePrefix("/ResourceTypes/")
                ok(discoveryService.getResourceType(name))
            }

            // Bulk endpoint
            relativePath == "/Bulk" && request.method == HttpMethod.POST -> {
                val handler = bulkHandler
                    ?: throw ScimException(status = 501, detail = "Bulk operations not supported")
                requireAuthorization { it.canBulk(context) }
                val bulkRequest = deserializeBody<com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest>(request)
                val result = handler.processBulk(bulkRequest, context)
                ok(result)
            }

            // Me endpoint
            relativePath == "/Me" -> handleMe(request, context)

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

        val pathAfterEndpoint = relativePath.removePrefix(handler.endpoint)

        // POST /{endpoint}/.search
        if (request.method == HttpMethod.POST && pathAfterEndpoint == "/.search") {
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
                ok(result)
            }

            HttpMethod.PATCH -> {
                requireNotNull(resourceId) { "PATCH requires a resource ID" }
                requireAuthorization { it.canUpdate(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                val patchRequest = deserializeBody<PatchRequest>(request)
                val result = handler.patch(ResourceId(resourceId), patchRequest, ifMatch, context)
                ok(result)
            }

            HttpMethod.DELETE -> {
                requireNotNull(resourceId) { "DELETE requires a resource ID" }
                requireAuthorization { it.canDelete(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                handler.delete(ResourceId(resourceId), ifMatch, context)
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
        handlers.firstOrNull { relativePath.startsWith(it.endpoint) }

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
