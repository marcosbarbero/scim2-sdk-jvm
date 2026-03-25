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
package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.schema.ResourceType
import com.marcosbarbero.scim2.core.domain.model.schema.Schema
import com.marcosbarbero.scim2.core.domain.model.schema.ServiceProviderConfig
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

class AsyncScimClient(
    private val delegate: ScimClient
) : AutoCloseable {

    suspend fun <T : ScimResource> create(endpoint: String, resource: T, type: KClass<T>): ScimResponse<T> =
        withContext(Dispatchers.IO) { delegate.create(endpoint, resource, type) }

    suspend fun <T : ScimResource> get(endpoint: String, id: String, type: KClass<T>): ScimResponse<T> =
        withContext(Dispatchers.IO) { delegate.get(endpoint, id, type) }

    suspend fun <T : ScimResource> replace(endpoint: String, id: String, resource: T, type: KClass<T>): ScimResponse<T> =
        withContext(Dispatchers.IO) { delegate.replace(endpoint, id, resource, type) }

    suspend fun <T : ScimResource> patch(endpoint: String, id: String, patchRequest: PatchRequest, type: KClass<T>): ScimResponse<T> =
        withContext(Dispatchers.IO) { delegate.patch(endpoint, id, patchRequest, type) }

    suspend fun delete(endpoint: String, id: String): Unit =
        withContext(Dispatchers.IO) { delegate.delete(endpoint, id) }

    suspend fun <T : ScimResource> search(endpoint: String, searchRequest: SearchRequest, type: KClass<T>): ScimResponse<ListResponse<T>> =
        withContext(Dispatchers.IO) { delegate.search(endpoint, searchRequest, type) }

    suspend fun bulk(bulkRequest: BulkRequest): ScimResponse<BulkResponse> =
        withContext(Dispatchers.IO) { delegate.bulk(bulkRequest) }

    suspend fun getServiceProviderConfig(): ScimResponse<ServiceProviderConfig> =
        withContext(Dispatchers.IO) { delegate.getServiceProviderConfig() }

    suspend fun getSchemas(): ScimResponse<ListResponse<Schema>> =
        withContext(Dispatchers.IO) { delegate.getSchemas() }

    suspend fun getResourceTypes(): ScimResponse<ListResponse<ResourceType>> =
        withContext(Dispatchers.IO) { delegate.getResourceTypes() }

    override fun close() {
        delegate.close()
    }
}

suspend inline fun <reified T : ScimResource> AsyncScimClient.create(endpoint: String, resource: T): ScimResponse<T> =
    create(endpoint, resource, T::class)

suspend inline fun <reified T : ScimResource> AsyncScimClient.get(endpoint: String, id: String): ScimResponse<T> =
    get(endpoint, id, T::class)

suspend inline fun <reified T : ScimResource> AsyncScimClient.replace(endpoint: String, id: String, resource: T): ScimResponse<T> =
    replace(endpoint, id, resource, T::class)

suspend inline fun <reified T : ScimResource> AsyncScimClient.patch(endpoint: String, id: String, patchRequest: PatchRequest): ScimResponse<T> =
    patch(endpoint, id, patchRequest, T::class)

suspend inline fun <reified T : ScimResource> AsyncScimClient.search(endpoint: String, searchRequest: SearchRequest): ScimResponse<ListResponse<T>> =
    search(endpoint, searchRequest, T::class)
