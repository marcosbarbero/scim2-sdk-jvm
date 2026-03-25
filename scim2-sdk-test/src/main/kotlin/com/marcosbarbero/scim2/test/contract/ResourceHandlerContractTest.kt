package com.marcosbarbero.scim2.test.contract

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Abstract contract test base class for [ResourceHandler] implementations.
 *
 * Any implementation can extend this to prove RFC 7644 compliance for basic CRUD + search operations.
 * Subclasses must provide:
 * - [createHandler] — factory for the handler under test
 * - [sampleResource] — creates a valid sample resource (without id/meta)
 * - [modifiedResource] — returns a modified copy of the given resource for replace tests
 */
abstract class ResourceHandlerContractTest<T : ScimResource> {

    abstract fun createHandler(): ResourceHandler<T>
    abstract fun sampleResource(): T
    abstract fun modifiedResource(original: T): T

    private lateinit var handler: ResourceHandler<T>
    private val context = ScimRequestContext()

    @BeforeEach
    fun setUp() {
        handler = createHandler()
    }

    @Test
    fun `create returns resource with id`() {
        val resource = sampleResource()
        val created = handler.create(resource, context)
        created.id.shouldNotBeNull()
    }

    @Test
    fun `create returns resource with meta`() {
        val resource = sampleResource()
        val created = handler.create(resource, context)
        created.meta.shouldNotBeNull()
        created.meta!!.created.shouldNotBeNull()
        created.meta!!.lastModified.shouldNotBeNull()
    }

    @Test
    fun `create returns resource with version in meta`() {
        val resource = sampleResource()
        val created = handler.create(resource, context)
        created.meta.shouldNotBeNull()
        created.meta!!.version.shouldNotBeNull()
    }

    @Test
    fun `get returns created resource`() {
        val created = handler.create(sampleResource(), context)
        val retrieved = handler.get(ResourceId(created.id!!), context)
        retrieved.id shouldBe created.id
    }

    @Test
    fun `get non-existent throws ResourceNotFoundException`() {
        shouldThrow<ResourceNotFoundException> {
            handler.get(ResourceId("non-existent-id"), context)
        }
    }

    @Test
    fun `replace updates resource`() {
        val created = handler.create(sampleResource(), context)
        val modified = modifiedResource(created)
        val replaced = handler.replace(ResourceId(created.id!!), modified, null, context)
        replaced.id shouldBe created.id
        replaced.meta.shouldNotBeNull()
        replaced.meta!!.lastModified.shouldNotBeNull()
    }

    @Test
    fun `replace non-existent throws ResourceNotFoundException`() {
        shouldThrow<ResourceNotFoundException> {
            handler.replace(ResourceId("non-existent-id"), sampleResource(), null, context)
        }
    }

    @Test
    fun `replace preserves created timestamp`() {
        val created = handler.create(sampleResource(), context)
        val modified = modifiedResource(created)
        val replaced = handler.replace(ResourceId(created.id!!), modified, null, context)
        replaced.meta!!.created shouldBe created.meta!!.created
    }

    @Test
    fun `replace updates lastModified timestamp`() {
        val created = handler.create(sampleResource(), context)
        val modified = modifiedResource(created)
        val replaced = handler.replace(ResourceId(created.id!!), modified, null, context)
        replaced.meta!!.lastModified shouldNotBe null
    }

    @Test
    fun `replace updates version`() {
        val created = handler.create(sampleResource(), context)
        val modified = modifiedResource(created)
        val replaced = handler.replace(ResourceId(created.id!!), modified, null, context)
        replaced.meta!!.version shouldNotBe created.meta!!.version
    }

    @Test
    fun `delete removes resource`() {
        val created = handler.create(sampleResource(), context)
        handler.delete(ResourceId(created.id!!), null, context)
        shouldThrow<ResourceNotFoundException> {
            handler.get(ResourceId(created.id!!), context)
        }
    }

    @Test
    fun `delete non-existent throws ResourceNotFoundException`() {
        shouldThrow<ResourceNotFoundException> {
            handler.delete(ResourceId("non-existent-id"), null, context)
        }
    }

    @Test
    fun `search returns list response`() {
        handler.create(sampleResource(), context)
        val result = handler.search(SearchRequest(), context)
        result.totalResults shouldBe 1
        result.resources shouldHaveSize 1
    }

    @Test
    fun `search with pagination respects startIndex and count`() {
        repeat(5) { handler.create(sampleResource(), context) }
        val result = handler.search(SearchRequest(startIndex = 2, count = 2), context)
        result.totalResults shouldBe 5
        result.resources shouldHaveSize 2
        result.startIndex shouldBe 2
    }

    @Test
    fun `search empty returns zero totalResults`() {
        val result = handler.search(SearchRequest(), context)
        result.totalResults shouldBe 0
        result.resources shouldHaveSize 0
    }

    @Test
    fun `create multiple then search returns all`() {
        repeat(3) { handler.create(sampleResource(), context) }
        val result = handler.search(SearchRequest(), context)
        result.totalResults shouldBe 3
        result.resources shouldHaveSize 3
    }

    @Test
    fun `search with count zero returns empty resources`() {
        handler.create(sampleResource(), context)
        val result = handler.search(SearchRequest(count = 0), context)
        result.totalResults shouldBe 1
        result.resources shouldHaveSize 0
    }

    @Test
    fun `search with startIndex beyond total returns empty resources`() {
        handler.create(sampleResource(), context)
        val result = handler.search(SearchRequest(startIndex = 100), context)
        result.totalResults shouldBe 1
        result.resources shouldHaveSize 0
    }

    @Test
    fun `create then get returns same id`() {
        val created = handler.create(sampleResource(), context)
        val retrieved = handler.get(ResourceId(created.id!!), context)
        retrieved.id shouldBe created.id
    }

    @Test
    fun `create two resources assigns different ids`() {
        val first = handler.create(sampleResource(), context)
        val second = handler.create(sampleResource(), context)
        first.id shouldNotBe second.id
    }

    @Test
    fun `delete then create allows reuse of search`() {
        val created = handler.create(sampleResource(), context)
        handler.delete(ResourceId(created.id!!), null, context)
        val result = handler.search(SearchRequest(), context)
        result.totalResults shouldBe 0
    }

    @Test
    fun `replace then get returns updated resource`() {
        val created = handler.create(sampleResource(), context)
        val modified = modifiedResource(created)
        handler.replace(ResourceId(created.id!!), modified, null, context)
        val retrieved = handler.get(ResourceId(created.id!!), context)
        retrieved.id shouldBe created.id
    }

    @Test
    fun `search returns correct itemsPerPage`() {
        repeat(5) { handler.create(sampleResource(), context) }
        val result = handler.search(SearchRequest(startIndex = 1, count = 3), context)
        result.itemsPerPage shouldBe 3
    }
}
