package com.marcosbarbero.scim2.server.engine

import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaginationEngineTest {

    private val faker = Faker()
    private val engine = PaginationEngine()

    @Test
    fun `paginate should return first page with default parameters`() {
        val items = (1..10).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 1, count = 5)

        result.totalResults shouldBe 10
        result.itemsPerPage shouldBe 5
        result.startIndex shouldBe 1
        result.resources shouldHaveSize 5
        result.resources shouldBe items.subList(0, 5)
    }

    @Test
    fun `paginate should return second page`() {
        val items = (1..10).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 6, count = 5)

        result.totalResults shouldBe 10
        result.itemsPerPage shouldBe 5
        result.startIndex shouldBe 6
        result.resources shouldBe items.subList(5, 10)
    }

    @Test
    fun `paginate should return empty when startIndex is beyond list size`() {
        val items = (1..5).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 100, count = 5)

        result.totalResults shouldBe 5
        result.resources.shouldBeEmpty()
        result.itemsPerPage shouldBe 0
    }

    @Test
    fun `paginate should return all items when count is null`() {
        val items = (1..5).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 1, count = null)

        result.totalResults shouldBe 5
        result.resources shouldHaveSize 5
    }

    @Test
    fun `paginate should handle startIndex less than 1`() {
        val items = (1..5).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 0, count = 3)

        result.startIndex shouldBe 1
        result.resources shouldHaveSize 3
        result.resources shouldBe items.subList(0, 3)
    }

    @Test
    fun `paginate should return partial page at end of list`() {
        val items = (1..7).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 6, count = 5)

        result.totalResults shouldBe 7
        result.itemsPerPage shouldBe 2
        result.resources shouldHaveSize 2
    }

    @Test
    fun `paginate should handle empty list`() {
        val result = engine.paginate(emptyList<String>(), startIndex = 1, count = 10)

        result.totalResults shouldBe 0
        result.resources.shouldBeEmpty()
    }

    @Test
    fun `paginate should accept custom totalResults`() {
        val items = (1..5).map { faker.name.firstName() }

        val result = engine.paginate(items, startIndex = 1, count = 5, totalResults = 100)

        result.totalResults shouldBe 100
        result.resources shouldHaveSize 5
    }
}
