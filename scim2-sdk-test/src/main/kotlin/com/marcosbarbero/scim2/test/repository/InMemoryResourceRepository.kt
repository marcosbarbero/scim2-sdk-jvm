package com.marcosbarbero.scim2.test.repository

import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.error.ResourceConflictException
import com.marcosbarbero.scim2.server.port.ResourceRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryResourceRepository<T : ScimResource>(
    private val copyWithIdAndMeta: (T, String, Meta) -> T
) : ResourceRepository<T> {

    private val store = ConcurrentHashMap<String, T>()

    override fun findById(id: String): T? = store[id]

    override fun create(resource: T): T {
        val id = resource.id ?: UUID.randomUUID().toString()
        if (store.containsKey(id)) {
            throw ResourceConflictException("Resource with id '$id' already exists")
        }
        val now = Instant.now()
        val meta = Meta(
            resourceType = resource.schemas.firstOrNull()?.substringAfterLast(":"),
            created = now,
            lastModified = now,
            version = ETag("W/\"${UUID.randomUUID()}\"")
        )
        val stored = copyWithIdAndMeta(resource, id, meta)
        store[id] = stored
        return stored
    }

    override fun replace(id: String, resource: T, version: String?): T {
        val existing = store[id]
            ?: throw ResourceNotFoundException("Resource not found: $id")
        if (version != null && existing.meta?.version != null && existing.meta?.version?.value != version) {
            throw ResourceConflictException("ETag mismatch")
        }
        val now = Instant.now()
        val meta = Meta(
            resourceType = existing.meta?.resourceType,
            created = existing.meta?.created,
            lastModified = now,
            version = ETag("W/\"${UUID.randomUUID()}\"")
        )
        val stored = copyWithIdAndMeta(resource, id, meta)
        store[id] = stored
        return stored
    }

    override fun delete(id: String, version: String?) {
        val existing = store[id]
            ?: throw ResourceNotFoundException("Resource not found: $id")
        if (version != null && existing.meta?.version != null && existing.meta?.version?.value != version) {
            throw ResourceConflictException("ETag mismatch")
        }
        store.remove(id)
    }

    override fun search(request: SearchRequest): ListResponse<T> {
        val all = store.values.toList()
        val startIndex = request.startIndex ?: 1
        val count = request.count ?: all.size
        val start = (startIndex - 1).coerceAtLeast(0)
        val end = (start + count).coerceAtMost(all.size)
        val page = if (start < all.size) all.subList(start, end) else emptyList()
        return ListResponse(
            totalResults = all.size,
            itemsPerPage = page.size,
            startIndex = startIndex,
            resources = page
        )
    }

    fun count(): Int = store.size

    fun clear() = store.clear()

    fun getAll(): List<T> = store.values.toList()
}
