package com.marcosbarbero.scim2.core.domain.model.search

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchModelsTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize SearchRequest`() {
        val request = SearchRequest(
            filter = "userName eq \"bjensen\"",
            sortBy = "userName",
            sortOrder = SortOrder.DESCENDING,
            startIndex = 1,
            count = 10,
            attributes = listOf("userName", "displayName")
        )

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue<SearchRequest>(json)

        deserialized.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:SearchRequest")
        deserialized.filter shouldBe "userName eq \"bjensen\""
        deserialized.sortBy shouldBe "userName"
        deserialized.sortOrder shouldBe SortOrder.DESCENDING
        deserialized.startIndex shouldBe 1
        deserialized.count shouldBe 10
        deserialized.attributes shouldBe listOf("userName", "displayName")
    }

    @Test
    fun `should serialize and deserialize ListResponse`() {
        val response = ListResponse(
            totalResults = 100,
            itemsPerPage = 10,
            startIndex = 1,
            resources = listOf(mapOf("userName" to "bjensen"), mapOf("userName" to "jsmith"))
        )

        val json = mapper.writeValueAsString(response)
        val deserialized = mapper.readValue<ListResponse<Map<String, String>>>(json)

        deserialized.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:ListResponse")
        deserialized.totalResults shouldBe 100
        deserialized.itemsPerPage shouldBe 10
        deserialized.resources.size shouldBe 2
    }

    @Test
    fun `SortOrder should have correct values`() {
        SortOrder.entries.map { it.name } shouldBe listOf("ASCENDING", "DESCENDING")
    }
}
