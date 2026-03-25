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
package com.marcosbarbero.scim2.server.engine

import com.marcosbarbero.scim2.core.domain.model.bulk.BulkOperation
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkOperationResponse
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse
import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource

import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import org.slf4j.LoggerFactory

class BulkEngine(
    private val handlers: List<ResourceHandler<*>>,
    private val serializer: ScimSerializer,
    private val config: ScimServerConfig
) {

    private val logger = LoggerFactory.getLogger(BulkEngine::class.java)

    fun execute(request: BulkRequest, context: ScimRequestContext): BulkResponse {
        require(request.operations.size <= config.bulkMaxOperations) {
            "Bulk request exceeds maximum operations: ${config.bulkMaxOperations}"
        }

        val bulkIdToResourceId = mutableMapOf<String, String>()
        val results = mutableListOf<BulkOperationResponse>()
        var errorCount = 0

        val failOnErrors = request.failOnErrors
        for (operation in request.operations) {
            if (failOnErrors != null && errorCount >= failOnErrors) {
                break
            }

            val result = executeOperation(operation, bulkIdToResourceId, context)
            if (result.status.toIntOrNull()?.let { it >= 400 } == true) {
                errorCount++
            }
            results.add(result)
        }

        return BulkResponse(operations = results)
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeOperation(
        operation: BulkOperation,
        bulkIdMap: MutableMap<String, String>,
        context: ScimRequestContext
    ): BulkOperationResponse {
        val method = operation.method.uppercase()
        val path = resolveBulkIdReferences(operation.path ?: "", bulkIdMap)
        val handler = findHandler(path) as? ResourceHandler<ScimResource>
            ?: return BulkOperationResponse(
                method = method,
                bulkId = operation.bulkId,
                status = "404",
                location = null
            )

        return try {
            when (method) {
                "POST" -> {
                    val data = operation.data
                        ?: return errorResponse(method, operation.bulkId, 400, "Missing data for POST")
                    val resource = serializer.deserialize(
                        serializer.serialize(data),
                        handler.resourceType.kotlin
                    )
                    val created = handler.create(resource, context)
                    val location = "${config.basePath}${handler.endpoint}/${created.id}"
                    val opBulkId = operation.bulkId
                    if (opBulkId != null) {
                        created.id?.let { bulkIdMap[opBulkId] = it }
                    }
                    BulkOperationResponse(
                        method = method,
                        bulkId = operation.bulkId,
                        status = "201",
                        location = location
                    )
                }

                "PUT" -> {
                    val resourceId = extractResourceId(path, handler.endpoint)
                    val data = operation.data
                        ?: return errorResponse(method, operation.bulkId, 400, "Missing data for PUT")
                    val resource = serializer.deserialize(
                        serializer.serialize(data),
                        handler.resourceType.kotlin
                    )
                    handler.replace(resourceId, resource, null, context)
                    val location = "${config.basePath}${handler.endpoint}/$resourceId"
                    BulkOperationResponse(
                        method = method,
                        bulkId = operation.bulkId,
                        status = "200",
                        location = location
                    )
                }

                "PATCH" -> {
                    val resourceId = extractResourceId(path, handler.endpoint)
                    val patchRequest = serializer.deserialize(
                        serializer.serialize(operation.data!!),
                        com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest::class
                    )
                    handler.patch(resourceId, patchRequest, null, context)
                    val location = "${config.basePath}${handler.endpoint}/$resourceId"
                    BulkOperationResponse(
                        method = method,
                        bulkId = operation.bulkId,
                        status = "200",
                        location = location
                    )
                }

                "DELETE" -> {
                    val resourceId = extractResourceId(path, handler.endpoint)
                    handler.delete(resourceId, null, context)
                    BulkOperationResponse(
                        method = method,
                        bulkId = operation.bulkId,
                        status = "204"
                    )
                }

                else -> errorResponse(method, operation.bulkId, 405, "Unsupported method: $method")
            }
        } catch (e: ScimException) {
            logger.debug("Bulk operation failed: {} {}", method, path, e)
            BulkOperationResponse(
                method = method,
                bulkId = operation.bulkId,
                status = e.status.toString()
            )
        }
    }

    private fun findHandler(path: String): ResourceHandler<*>? {
        val normalizedPath = if (path.startsWith(config.basePath)) {
            path.removePrefix(config.basePath)
        } else {
            path
        }
        return handlers.firstOrNull { normalizedPath.startsWith(it.endpoint) }
    }

    private fun extractResourceId(path: String, endpoint: String): String {
        val normalizedPath = if (path.startsWith(config.basePath)) {
            path.removePrefix(config.basePath)
        } else {
            path
        }
        return normalizedPath.removePrefix(endpoint).removePrefix("/")
    }

    private fun resolveBulkIdReferences(path: String, bulkIdMap: Map<String, String>): String {
        var resolved = path
        val regex = Regex("bulkId:([^/\"\\s]+)")
        regex.findAll(path).forEach { match ->
            val bulkId = match.groupValues[1]
            bulkIdMap[bulkId]?.let { resourceId ->
                resolved = resolved.replace("bulkId:$bulkId", resourceId)
            }
        }
        return resolved
    }

    private fun errorResponse(method: String, bulkId: String?, status: Int, detail: String): BulkOperationResponse =
        BulkOperationResponse(
            method = method,
            bulkId = bulkId,
            status = status.toString()
        )
}
