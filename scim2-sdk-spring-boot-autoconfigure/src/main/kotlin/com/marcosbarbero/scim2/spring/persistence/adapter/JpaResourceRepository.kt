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
package com.marcosbarbero.scim2.spring.persistence.adapter

import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.model.search.SortOrder
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.filter.FilterEngine
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.spring.persistence.entity.ScimResourceEntity
import com.marcosbarbero.scim2.spring.persistence.repository.ScimResourceJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.time.Instant
import java.util.UUID

class JpaResourceRepository<T : ScimResource>(
    private val jpaRepository: ScimResourceJpaRepository,
    private val serializer: ScimSerializer,
    private val resourceType: Class<T>,
    private val resourceTypeName: String,
    private val filterObjectMapper: ObjectMapper = JacksonScimSerializer.defaultObjectMapper(),
    private val maxFetchSize: Int = DEFAULT_MAX_FETCH_SIZE,
) : ResourceRepository<T> {

    private val logger = LoggerFactory.getLogger(JpaResourceRepository::class.java)

    override fun create(resource: T): T {
        // Idempotency: if a resource with the same externalId already exists, update it
        val existingByExternalId = resource.externalId?.let {
            jpaRepository.findByResourceTypeAndExternalId(resourceTypeName, it)
        }
        if (existingByExternalId != null) {
            return replace(existingByExternalId.id, resource, null)
        }

        val id = resource.id ?: UUID.randomUUID().toString()
        val now = Instant.now()
        val meta = Meta(
            resourceType = resourceTypeName,
            created = now,
            lastModified = now,
            version = ETag("W/\"1\""),
        )
        val withMeta = copyWithIdAndMeta(resource, id, meta)
        val entity = ScimResourceEntity(
            id = id,
            resourceType = resourceTypeName,
            externalId = resource.externalId,
            displayName = extractDisplayName(resource),
            resourceJson = serializer.serializeToString(withMeta),
            version = 1,
            created = now,
            lastModified = now,
        )
        jpaRepository.save(entity)
        return withMeta
    }

    override fun findById(id: String): T? {
        val entity = jpaRepository.findById(id).orElse(null) ?: return null
        if (entity.resourceType != resourceTypeName) return null
        return serializer.deserializeFromString(entity.resourceJson, resourceType.kotlin)
    }

    override fun replace(id: String, resource: T, version: String?): T {
        val entity = jpaRepository.findById(id).orElse(null)
            ?: throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        if (entity.resourceType != resourceTypeName) {
            throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        }
        val now = Instant.now()
        val newVersion = entity.version + 1
        val meta = Meta(
            resourceType = resourceTypeName,
            created = entity.created,
            lastModified = now,
            version = ETag("W/\"$newVersion\""),
        )
        val withMeta = copyWithIdAndMeta(resource, id, meta)
        entity.resourceJson = serializer.serializeToString(withMeta)
        entity.externalId = resource.externalId
        entity.displayName = extractDisplayName(resource)
        entity.version = newVersion
        entity.lastModified = now
        jpaRepository.save(entity)
        return withMeta
    }

    override fun delete(id: String, version: String?) {
        val deleted = jpaRepository.deleteByIdAndResourceType(id, resourceTypeName)
        if (deleted == 0L) {
            throw ResourceNotFoundException("${resourceType.simpleName} not found: $id")
        }
    }

    override fun search(request: SearchRequest): ListResponse<T> {
        val startIndex = request.startIndex ?: 1
        val count = request.count ?: 100

        val filterString = request.filter
        if (!filterString.isNullOrBlank()) {
            return searchWithFilter(filterString, startIndex, count, request.sortBy, request.sortOrder)
        }

        val sortDirection = if (request.sortOrder == SortOrder.DESCENDING) {
            Sort.Direction.DESC
        } else {
            Sort.Direction.ASC
        }
        val sortProperty = request.sortBy ?: "created"
        val pageable = PageRequest.of(
            ((startIndex - 1) / count).coerceAtLeast(0),
            count,
            sortDirection,
            sortProperty,
        )
        val page = jpaRepository.findByResourceType(resourceTypeName, pageable)
        val resources = page.content.map {
            serializer.deserializeFromString(it.resourceJson, resourceType.kotlin)
        }
        return ListResponse(
            totalResults = page.totalElements.toInt(),
            itemsPerPage = resources.size,
            startIndex = startIndex,
            resources = resources,
        )
    }

    private fun searchWithFilter(
        filterString: String,
        startIndex: Int,
        count: Int,
        sortBy: String?,
        sortOrder: SortOrder?,
    ): ListResponse<T> {
        logger.warn(
            "Applying SCIM filter in memory — all resources of type {} loaded from database. " +
                "Consider SQL-level filtering for large datasets.",
            resourceTypeName,
        )
        val pageable = PageRequest.of(0, maxFetchSize, Sort.by(Sort.Direction.ASC, "created"))
        val fetchedPage = jpaRepository.findByResourceType(resourceTypeName, pageable)
        val allResources = fetchedPage.content.map {
            serializer.deserializeFromString(it.resourceJson, resourceType.kotlin)
        }
        val filtered = FilterEngine.filter(allResources, filterString, filterObjectMapper)
        val sorted = applySorting(filtered, sortBy, sortOrder)
        val start = (startIndex - 1).coerceAtLeast(0)
        val end = (start + count).coerceAtMost(sorted.size)
        val page = if (start < sorted.size) sorted.subList(start, end) else emptyList()
        return ListResponse(
            totalResults = sorted.size,
            itemsPerPage = page.size,
            startIndex = startIndex,
            resources = page,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun applySorting(resources: List<T>, sortBy: String?, sortOrder: SortOrder?): List<T> {
        if (sortBy == null) return resources
        val descending = sortOrder == SortOrder.DESCENDING
        return resources.sortedWith(
            Comparator { a, b ->
                val mapA = filterObjectMapper.convertValue(a, Map::class.java) as Map<String, Any?>
                val mapB = filterObjectMapper.convertValue(b, Map::class.java) as Map<String, Any?>
                val valA = mapA[sortBy]
                val valB = mapB[sortBy]
                val cmp = compareValues(valA as? Comparable<Any>, valB as? Comparable<Any>)
                if (descending) -cmp else cmp
            },
        )
    }

    companion object {
        const val DEFAULT_MAX_FETCH_SIZE = 10_000
    }

    private fun extractDisplayName(resource: T): String? = when (resource) {
        is User -> resource.displayName ?: resource.userName
        is Group -> resource.displayName
        else -> null
    }

    private fun copyWithIdAndMeta(resource: T, id: String, meta: Meta): T {
        val mapper = JsonMapper.builder().build()
        val json = serializer.serializeToString(resource)
        val metaJson = serializer.serializeToString(meta)
        val node = mapper.readTree(json) as ObjectNode
        node.put("id", id)
        node.set("meta", mapper.readTree(metaJson))
        val updatedJson = mapper.writeValueAsString(node)
        return serializer.deserializeFromString(updatedJson, resourceType.kotlin)
    }
}
