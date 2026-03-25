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
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.spring.persistence.entity.ScimResourceEntity
import com.marcosbarbero.scim2.spring.persistence.repository.ScimResourceJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.Instant
import java.util.UUID

class JpaResourceRepository<T : ScimResource>(
    private val jpaRepository: ScimResourceJpaRepository,
    private val serializer: ScimSerializer,
    private val resourceType: Class<T>,
    private val resourceTypeName: String
) : ResourceRepository<T> {

    override fun create(resource: T): T {
        val id = resource.id ?: UUID.randomUUID().toString()
        val now = Instant.now()
        val meta = Meta(
            resourceType = resourceTypeName,
            created = now,
            lastModified = now,
            version = ETag("W/\"1\"")
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
            lastModified = now
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
            version = ETag("W/\"$newVersion\"")
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
            sortProperty
        )
        val page = jpaRepository.findByResourceType(resourceTypeName, pageable)
        val resources = page.content.map {
            serializer.deserializeFromString(it.resourceJson, resourceType.kotlin)
        }
        return ListResponse(
            totalResults = page.totalElements.toInt(),
            itemsPerPage = resources.size,
            startIndex = startIndex,
            resources = resources
        )
    }

    private fun extractDisplayName(resource: T): String? = when (resource) {
        is User -> resource.displayName ?: resource.userName
        is Group -> resource.displayName
        else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun copyWithIdAndMeta(resource: T, id: String, meta: Meta): T = when (resource) {
        is User -> resource.copy(id = id, meta = meta) as T
        is Group -> resource.copy(id = id, meta = meta) as T
        else -> resource // fallback; extensions would need their own copy logic
    }
}
