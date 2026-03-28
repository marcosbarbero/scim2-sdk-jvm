/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marcosbarbero.scim2.server.adapter.http

import com.marcosbarbero.scim2.core.domain.model.error.InvalidFilterException
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
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
    private val tracer: ScimTracer = NoOpScimTracer,
) {

    private val logger = LoggerFactory.getLogger(ScimEndpointDispatcher::class.java)
    private val sortedInterceptors = interceptors.sortedBy { it.order }
    private val normalizedBaseUrl: String? = config.baseUrl?.trimEnd('/')

    fun dispatch(request: ScimHttpRequest): ScimHttpResponse {
        val context = request.toScimRequestContext()
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
                    mapOf("endpoint" to endpoint, "method" to method),
                ) {
                    route(processedRequest, context)
                }
            } catch (e: ScimException) {
                val processedException = sortedInterceptors.fold(e) { ex, interceptor ->
                    interceptor.onError(processedRequest, ex, context)
                }
                processedException.toErrorResponse(processedRequest)
            } catch (e: Exception) {
                logger.error("Unexpected error processing request: {} {}", request.method, request.path, e)
                val fallback = ScimException(status = 500, detail = "Internal server error")
                fallback.toErrorResponse(processedRequest)
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
        findHandler(relativePath)?.let { return it.endpoint }
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
        findHandler(relativePath)?.let { return it.resourceType.simpleName ?: "Unknown" }
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
                if (!config.bulkEnabled) throw ScimException(status = 501, detail = "Bulk operations are not supported")
                val handler = bulkHandler
                    ?: throw ScimException(status = 501, detail = "Bulk operations not supported")
                requireAuthorization { it.canBulk(context) }
                request.body?.let { body ->
                    if (body.size > config.bulkMaxPayloadSize) {
                        throw ScimException(status = 413, detail = "Bulk request payload exceeds maximum size of ${config.bulkMaxPayloadSize} bytes")
                    }
                }
                val bulkRequest = deserializeBody<com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest>(request)
                val result = handler.processBulk(bulkRequest, context)
                val failureCount = result.operations.count { !it.status.startsWith("2") }
                eventPublisher.publish(
                    BulkOperationCompletedEvent(
                        correlationId = tracer.currentCorrelationId(),
                        operationCount = result.operations.size,
                        failureCount = failureCount,
                    ),
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
        context: ScimRequestContext,
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
            validateSearchRequest(searchRequest)
            val effectiveRequest = applyPaginationDefaults(searchRequest)
            val result = handler.search(effectiveRequest, context)
            val capped = capSearchResults(result)
            return okWithEnrichedListResponse(capped, handler, resourceTypeName)
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
                val relativeLocation = created.id?.let { "${config.basePath}${handler.endpoint}/$it" }
                val absoluteLocation = if (relativeLocation != null) normalizedBaseUrl?.let { "$it$relativeLocation" } else null
                val locationHeader = absoluteLocation ?: relativeLocation ?: "${config.basePath}${handler.endpoint}"
                var bytes = serializer.serialize(created)
                bytes = enrichSerializedMetaLocation(bytes, absoluteLocation, resourceTypeName)
                val enrichedBytes = enrichMemberRefs(bytes)
                eventPublisher.publish(
                    ResourceCreatedEvent(
                        resourceType = resourceTypeName,
                        resourceId = created.id ?: "",
                        correlationId = tracer.currentCorrelationId(),
                    ),
                )
                ScimHttpResponse.created(enrichedBytes, locationHeader)
            }

            HttpMethod.GET -> {
                if (resourceId.isNullOrEmpty()) {
                    // Search via GET
                    requireAuthorization { it.canSearch(handler.endpoint, context) }
                    val searchRequest = request.toSearchRequest()
                    validateSearchRequest(searchRequest)
                    val effectiveRequest = applyPaginationDefaults(searchRequest)
                    val result = handler.search(effectiveRequest, context)
                    val capped = capSearchResults(result)
                    okWithEnrichedListResponse(capped, handler, resourceTypeName)
                } else {
                    requireAuthorization { it.canRead(handler.endpoint, resourceId, context) }
                    val result = handler.get(resourceId, context)
                    val absoluteLocation = result.id?.let { rid -> normalizedBaseUrl?.let { "$it${config.basePath}${handler.endpoint}/$rid" } }
                    // Compute ETag BEFORE meta.location enrichment for stable ETags
                    val etag = etagEngine.generateETag(result)
                    val ifNoneMatch = etagEngine.extractIfNoneMatch(request)
                    if (ifNoneMatch != null && etag == ifNoneMatch) {
                        ScimHttpResponse(status = 304)
                    } else {
                        val bytes = serializer.serialize(result)
                        val enrichedBytes = enrichSerializedMetaLocation(bytes, absoluteLocation, resourceTypeName)
                        ScimHttpResponse.ok(
                            enrichedBytes,
                            headers = if (config.etagEnabled) mapOf("ETag" to etag.value) else emptyMap(),
                        )
                    }
                }
            }

            HttpMethod.PUT -> {
                requireNotNull(resourceId) { "PUT requires a resource ID" }
                requireAuthorization { it.canUpdate(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                val resource = deserializeBody(request, handler.resourceType)
                val result = handler.replace(resourceId, resource, ifMatch?.value, context)
                val absoluteLocation = result.id?.let { rid -> normalizedBaseUrl?.let { "$it${config.basePath}${handler.endpoint}/$rid" } }
                eventPublisher.publish(
                    ResourceReplacedEvent(
                        resourceType = resourceTypeName,
                        resourceId = resourceId,
                        correlationId = tracer.currentCorrelationId(),
                    ),
                )
                okWithEnrichedBytes(result, absoluteLocation, resourceTypeName)
            }

            HttpMethod.PATCH -> {
                if (!config.patchEnabled) throw ScimException(status = 501, detail = "PATCH operations are not supported")
                requireNotNull(resourceId) { "PATCH requires a resource ID" }
                requireAuthorization { it.canUpdate(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                val patchRequest = deserializeBody<PatchRequest>(request)
                val result = handler.patch(resourceId, patchRequest, ifMatch?.value, context)
                val absoluteLocation = result.id?.let { rid -> normalizedBaseUrl?.let { "$it${config.basePath}${handler.endpoint}/$rid" } }
                eventPublisher.publish(
                    ResourcePatchedEvent(
                        resourceType = resourceTypeName,
                        resourceId = resourceId,
                        correlationId = tracer.currentCorrelationId(),
                        operationCount = patchRequest.operations.size,
                    ),
                )
                okWithEnrichedBytes(result, absoluteLocation, resourceTypeName)
            }

            HttpMethod.DELETE -> {
                requireNotNull(resourceId) { "DELETE requires a resource ID" }
                requireAuthorization { it.canDelete(handler.endpoint, resourceId, context) }
                val ifMatch = etagEngine.extractIfMatch(request)
                handler.delete(resourceId, ifMatch?.value, context)
                eventPublisher.publish(
                    ResourceDeletedEvent(
                        resourceType = resourceTypeName,
                        resourceId = resourceId,
                        correlationId = tracer.currentCorrelationId(),
                    ),
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
                val result = handler.replaceMe(context, resource, ifMatch?.value)
                ok(result)
            }

            HttpMethod.PATCH -> {
                val ifMatch = etagEngine.extractIfMatch(request)
                val patchRequest = deserializeBody<PatchRequest>(request)
                val result = handler.patchMe(context, patchRequest, ifMatch?.value)
                ok(result)
            }

            HttpMethod.DELETE -> {
                val ifMatch = etagEngine.extractIfMatch(request)
                handler.deleteMe(context, ifMatch?.value)
                ScimHttpResponse.noContent()
            }

            HttpMethod.POST -> throw ScimException(status = 405, detail = "POST not allowed on /Me")
        }
    }

    private fun validateSearchRequest(searchRequest: SearchRequest) {
        if (!config.filterEnabled && searchRequest.filter != null) {
            throw InvalidFilterException("Filtering is not supported")
        }
        if (!config.sortEnabled && searchRequest.sortBy != null) {
            throw ScimException(status = 400, detail = "Sorting is not supported")
        }
    }

    private fun applyPaginationDefaults(searchRequest: SearchRequest): SearchRequest {
        val effectiveCount = (searchRequest.count ?: config.defaultPageSize).coerceAtMost(config.maxPageSize)
        return searchRequest.copy(count = effectiveCount)
    }

    private fun <T> capSearchResults(result: ListResponse<T>): ListResponse<T> = if (result.resources.size > config.filterMaxResults) {
        result.copy(
            resources = result.resources.take(config.filterMaxResults),
            itemsPerPage = config.filterMaxResults,
        )
    } else {
        result
    }

    /**
     * Enriches already-serialized JSON bytes with meta.location (and meta.resourceType if absent).
     * Operates on wire format to avoid lossy domain-object round-trips.
     * Returns original bytes unchanged when location is null or resource.id is null.
     */
    private fun enrichSerializedMetaLocation(
        bytes: ByteArray,
        location: String?,
        resourceType: String?,
    ): ByteArray {
        location ?: return bytes
        return serializer.enrichMetaLocation(bytes, location, resourceType)
    }

    private fun <T : Any> okWithEnrichedBytes(
        resource: T,
        absoluteLocation: String?,
        resourceType: String?,
    ): ScimHttpResponse {
        var bytes = serializer.serialize(resource)
        bytes = enrichSerializedMetaLocation(bytes, absoluteLocation, resourceType)
        bytes = enrichMemberRefs(bytes)
        return ScimHttpResponse.ok(bytes)
    }

    private fun enrichMemberRefs(bytes: ByteArray): ByteArray {
        normalizedBaseUrl ?: return bytes
        return serializer.enrichMemberRefs(bytes, "$normalizedBaseUrl${config.basePath}")
    }

    private fun <T : ScimResource> okWithEnrichedListResponse(
        result: ListResponse<T>,
        handler: ResourceHandler<*>,
        resourceTypeName: String,
    ): ScimHttpResponse {
        if (normalizedBaseUrl == null) return ok(result)
        val baseScimUrl = "$normalizedBaseUrl${config.basePath}"
        val enrichedResources = result.resources.map { resource ->
            var bytes = serializer.serialize(resource)
            if (resource.id != null) {
                val location = "$baseScimUrl${handler.endpoint}/${resource.id}"
                bytes = serializer.enrichMetaLocation(bytes, location, resourceTypeName)
            }
            bytes = serializer.enrichMemberRefs(bytes, baseScimUrl)
            bytes
        }
        // Build the ListResponse JSON manually with enriched resource bytes
        val listBytes = serializer.serialize(result.copy(resources = emptyList()))
        return ScimHttpResponse.ok(assembleListResponseBytes(listBytes, enrichedResources))
    }

    /**
     * Replaces the empty "Resources" array in a serialized ListResponse with
     * pre-enriched resource JSON byte arrays.
     */
    private fun assembleListResponseBytes(
        listResponseBytes: ByteArray,
        resourceBytesList: List<ByteArray>,
    ): ByteArray {
        if (resourceBytesList.isEmpty()) return listResponseBytes
        val listJson = String(listResponseBytes, Charsets.UTF_8)
        // Replace "Resources":[] with the actual enriched resources
        val resourcesJson = resourceBytesList.joinToString(",") { String(it, Charsets.UTF_8) }
        val replaced = listJson.replace("\"Resources\":[]", "\"Resources\":[$resourcesJson]")
        return replaced.toByteArray(Charsets.UTF_8)
    }

    private fun findHandler(relativePath: String): ResourceHandler<*>? = handlers.firstOrNull { relativePath.lowercase().startsWith(it.endpoint.lowercase()) }

    private fun ScimHttpRequest.toScimRequestContext(): ScimRequestContext {
        val requestedAttrs = queryParam("attributes")
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()
        val excludedAttrs = queryParam("excludedAttributes")
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()

        return identityResolver?.resolve(this)?.copy(
            requestedAttributes = requestedAttrs,
            excludedAttributes = excludedAttrs,
        ) ?: ScimRequestContext(
            requestedAttributes = requestedAttrs,
            excludedAttributes = excludedAttrs,
        )
    }

    private fun ScimHttpRequest.toSearchRequest(): SearchRequest = SearchRequest(
        filter = queryParam("filter"),
        sortBy = queryParam("sortBy"),
        sortOrder = queryParam("sortOrder")?.let {
            com.marcosbarbero.scim2.core.domain.model.search.SortOrder.entries
                .firstOrNull { order -> order.value.equals(it, ignoreCase = true) }
        },
        startIndex = queryParam("startIndex")?.toIntOrNull(),
        count = queryParam("count")?.toIntOrNull(),
        attributes = queryParam("attributes")
            ?.split(",")
            ?.map { it.trim() }
            ?.takeIf { it.isNotEmpty() },
        excludedAttributes = queryParam("excludedAttributes")
            ?.split(",")
            ?.map { it.trim() }
            ?.takeIf { it.isNotEmpty() },
    )

    private fun <T : Any> ok(body: T): ScimHttpResponse = ScimHttpResponse.ok(serializer.serialize(body))

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

    private fun ScimException.toErrorResponse(request: ScimHttpRequest): ScimHttpResponse {
        val acceptsProblemJson = request.headers.entries
            .firstOrNull { it.key.equals("Accept", ignoreCase = true) }
            ?.value
            ?.any { it.contains("application/problem+json") } == true

        val body = if (acceptsProblemJson) {
            serializer.serialize(toScimProblemDetail())
        } else {
            serializer.serialize(toScimError())
        }

        val contentType = if (acceptsProblemJson) "application/problem+json" else "application/scim+json"
        return ScimHttpResponse.error(status, body, mapOf("Content-Type" to contentType))
    }
}
