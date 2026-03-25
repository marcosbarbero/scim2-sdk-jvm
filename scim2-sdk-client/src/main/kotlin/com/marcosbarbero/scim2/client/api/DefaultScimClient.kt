package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.client.error.ScimClientException
import com.marcosbarbero.scim2.client.port.AuthenticationStrategy
import com.marcosbarbero.scim2.client.port.HttpRequest
import com.marcosbarbero.scim2.client.port.HttpResponse
import com.marcosbarbero.scim2.client.port.HttpTransport
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse
import com.marcosbarbero.scim2.core.domain.model.error.ScimError
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.Schema
import com.marcosbarbero.scim2.core.domain.model.schema.ServiceProviderConfig
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import kotlin.reflect.KClass

internal class DefaultScimClient(
    private val baseUrl: String,
    private val transport: HttpTransport,
    private val serializer: ScimSerializer,
    private val authentication: AuthenticationStrategy?,
    private val defaultHeaders: Map<String, String>
) : ScimClient {

    companion object {
        private const val CONTENT_TYPE = "Content-Type"
        private const val ACCEPT = "Accept"
        private const val SCIM_MEDIA_TYPE = "application/scim+json"
        private const val ETAG_HEADER = "ETag"
        private const val LOCATION_HEADER = "Location"
    }

    override fun <T : ScimResource> create(endpoint: String, resource: T, type: KClass<T>): ScimResponse<T> {
        val body = serializer.serialize(resource)
        val request = buildRequest("POST", "$baseUrl$endpoint", body)
        val response = executeRequest(request)
        checkForErrors(response)
        val value = serializer.deserialize(requireResponseBody(response), type)
        return toScimResponse(value, response)
    }

    override fun <T : ScimResource> get(endpoint: String, id: String, type: KClass<T>): ScimResponse<T> {
        val request = buildRequest("GET", "$baseUrl$endpoint/$id")
        val response = executeRequest(request)
        checkForErrors(response)
        val value = serializer.deserialize(requireResponseBody(response), type)
        return toScimResponse(value, response)
    }

    override fun <T : ScimResource> replace(endpoint: String, id: String, resource: T, type: KClass<T>): ScimResponse<T> {
        val body = serializer.serialize(resource)
        val request = buildRequest("PUT", "$baseUrl$endpoint/$id", body)
        val response = executeRequest(request)
        checkForErrors(response)
        val value = serializer.deserialize(requireResponseBody(response), type)
        return toScimResponse(value, response)
    }

    override fun <T : ScimResource> patch(endpoint: String, id: String, patchRequest: PatchRequest, type: KClass<T>): ScimResponse<T> {
        val body = serializer.serialize(patchRequest)
        val request = buildRequest("PATCH", "$baseUrl$endpoint/$id", body)
        val response = executeRequest(request)
        checkForErrors(response)
        val value = serializer.deserialize(requireResponseBody(response), type)
        return toScimResponse(value, response)
    }

    override fun delete(endpoint: String, id: String) {
        val request = buildRequest("DELETE", "$baseUrl$endpoint/$id")
        val response = executeRequest(request)
        checkForErrors(response)
    }

    override fun <T : ScimResource> search(endpoint: String, searchRequest: SearchRequest, type: KClass<T>): ScimResponse<ListResponse<T>> {
        val body = serializer.serialize(searchRequest)
        val request = buildRequest("POST", "$baseUrl$endpoint/.search", body)
        val response = executeRequest(request)
        checkForErrors(response)
        @Suppress("UNCHECKED_CAST")
        val listResponse = serializer.deserialize(requireResponseBody(response), ListResponse::class) as ListResponse<T>
        return toScimResponse(listResponse, response)
    }

    override fun bulk(bulkRequest: BulkRequest): ScimResponse<BulkResponse> {
        val body = serializer.serialize(bulkRequest)
        val request = buildRequest("POST", "$baseUrl/Bulk", body)
        val response = executeRequest(request)
        checkForErrors(response)
        val value = serializer.deserialize(requireResponseBody(response), BulkResponse::class)
        return toScimResponse(value, response)
    }

    override fun getServiceProviderConfig(): ScimResponse<ServiceProviderConfig> {
        val request = buildRequest("GET", "$baseUrl/ServiceProviderConfig")
        val response = executeRequest(request)
        checkForErrors(response)
        val value = serializer.deserialize(requireResponseBody(response), ServiceProviderConfig::class)
        return toScimResponse(value, response)
    }

    override fun getSchemas(): ScimResponse<ListResponse<Schema>> {
        val request = buildRequest("GET", "$baseUrl/Schemas")
        val response = executeRequest(request)
        checkForErrors(response)
        @Suppress("UNCHECKED_CAST")
        val value = serializer.deserialize(requireResponseBody(response), ListResponse::class) as ListResponse<Schema>
        return toScimResponse(value, response)
    }

    override fun getResourceTypes(): ScimResponse<ListResponse<ResourceType>> {
        val request = buildRequest("GET", "$baseUrl/ResourceTypes")
        val response = executeRequest(request)
        checkForErrors(response)
        @Suppress("UNCHECKED_CAST")
        val value = serializer.deserialize(requireResponseBody(response), ListResponse::class) as ListResponse<ResourceType>
        return toScimResponse(value, response)
    }

    override fun close() {
        transport.close()
    }

    private fun buildRequest(method: String, url: String, body: ByteArray? = null): HttpRequest {
        val headers = buildMap {
            putAll(defaultHeaders)
            put(ACCEPT, SCIM_MEDIA_TYPE)
            body?.let { put(CONTENT_TYPE, SCIM_MEDIA_TYPE) }
        }
        return HttpRequest(method = method, url = url, headers = headers, body = body)
    }

    private fun executeRequest(request: HttpRequest): HttpResponse {
        val authenticatedRequest = authentication?.authenticate(request) ?: request
        return transport.execute(authenticatedRequest)
    }

    private fun checkForErrors(response: HttpResponse) {
        if (response.statusCode in 400..599) {
            val scimError = response.body?.let {
                try {
                    serializer.deserialize(it, ScimError::class)
                } catch (_: Exception) {
                    null
                }
            }
            throw ScimClientException(
                statusCode = response.statusCode,
                scimError = scimError
            )
        }
    }

    private fun requireResponseBody(response: HttpResponse): ByteArray {
        return response.body
            ?: throw ScimClientException(
                statusCode = response.statusCode,
                message = "Expected response body but got empty response (status=${response.statusCode}, headers=${response.headers.keys})"
            )
    }

    private fun <T> toScimResponse(value: T, response: HttpResponse): ScimResponse<T> {
        return ScimResponse(
            value = value,
            statusCode = response.statusCode,
            headers = response.headers,
            etag = response.headers.entries
                .firstOrNull { it.key.equals(ETAG_HEADER, ignoreCase = true) }
                ?.value?.firstOrNull(),
            location = response.headers.entries
                .firstOrNull { it.key.equals(LOCATION_HEADER, ignoreCase = true) }
                ?.value?.firstOrNull()
        )
    }
}
