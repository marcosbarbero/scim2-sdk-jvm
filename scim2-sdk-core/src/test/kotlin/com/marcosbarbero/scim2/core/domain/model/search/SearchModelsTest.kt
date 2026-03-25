package com.marcosbarbero.scim2.core.domain.model.search

import com.marcosbarbero.scim2.core.domain.ScimUrns
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchModelsTest {

    private val faker = Faker()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize SearchRequest`() {
        val userName = faker.name.firstName().lowercase()
        val request = SearchRequest(
            filter = "userName eq \"$userName\"",
            sortBy = "userName",
            sortOrder = SortOrder.DESCENDING,
            startIndex = 1,
            count = 10,
            attributes = listOf("userName", "displayName")
        )

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue<SearchRequest>(json)

        deserialized.schemas shouldBe listOf(ScimUrns.SEARCH_REQUEST)
        deserialized.filter shouldBe "userName eq \"$userName\""
        deserialized.sortBy shouldBe "userName"
        deserialized.sortOrder shouldBe SortOrder.DESCENDING
        deserialized.startIndex shouldBe 1
        deserialized.count shouldBe 10
        deserialized.attributes shouldBe listOf("userName", "displayName")
    }

    @Test
    fun `should serialize and deserialize ListResponse`() {
        val userName1 = faker.name.firstName().lowercase()
        val userName2 = faker.name.firstName().lowercase()
        val response = ListResponse(
            totalResults = 100,
            itemsPerPage = 10,
            startIndex = 1,
            resources = listOf(mapOf("userName" to userName1), mapOf("userName" to userName2))
        )

        val json = mapper.writeValueAsString(response)
        val deserialized = mapper.readValue<ListResponse<Map<String, String>>>(json)

        deserialized.schemas shouldBe listOf(ScimUrns.LIST_RESPONSE)
        deserialized.totalResults shouldBe 100
        deserialized.itemsPerPage shouldBe 10
        deserialized.resources.size shouldBe 2
    }

    @Test
    fun `SortOrder should have correct values`() {
        SortOrder.entries.map { it.name } shouldBe listOf("ASCENDING", "DESCENDING")
    }
}
