package com.marcosbarbero.scim2.core.domain.model.bulk

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BulkModelsTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize BulkRequest`() {
        val request = BulkRequest(
            failOnErrors = 1,
            operations = listOf(
                BulkOperation(method = "POST", path = "/Users", bulkId = "qwerty", data = mapOf("userName" to "bjensen")),
                BulkOperation(method = "DELETE", path = "/Users/123")
            )
        )

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue<BulkRequest>(json)

        deserialized.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:BulkRequest")
        deserialized.failOnErrors shouldBe 1
        deserialized.operations.size shouldBe 2
        deserialized.operations[0].method shouldBe "POST"
        deserialized.operations[0].bulkId shouldBe "qwerty"
    }

    @Test
    fun `should serialize and deserialize BulkResponse`() {
        val response = BulkResponse(
            operations = listOf(
                BulkOperationResponse(
                    method = "POST",
                    bulkId = "qwerty",
                    status = "201",
                    location = "https://example.com/v2/Users/123"
                ),
                BulkOperationResponse(method = "DELETE", status = "204")
            )
        )

        val json = mapper.writeValueAsString(response)
        val deserialized = mapper.readValue<BulkResponse>(json)

        deserialized.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:BulkResponse")
        deserialized.operations.size shouldBe 2
        deserialized.operations[0].status shouldBe "201"
        deserialized.operations[0].location shouldBe "https://example.com/v2/Users/123"
    }
}
