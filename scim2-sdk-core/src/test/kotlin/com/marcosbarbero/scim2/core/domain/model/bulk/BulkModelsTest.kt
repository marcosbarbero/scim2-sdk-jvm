package com.marcosbarbero.scim2.core.domain.model.bulk

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BulkModelsTest {

    private val faker = Faker()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize BulkRequest`() {
        val userName = faker.name.firstName().lowercase()
        val bulkId = java.util.UUID.randomUUID().toString()
        val userId = java.util.UUID.randomUUID().toString()
        val request = BulkRequest(
            failOnErrors = 1,
            operations = listOf(
                BulkOperation(method = "POST", path = "/Users", bulkId = bulkId, data = mapper.valueToTree(mapOf("userName" to userName))),
                BulkOperation(method = "DELETE", path = "/Users/$userId")
            )
        )

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue<BulkRequest>(json)

        deserialized.schemas shouldBe listOf(ScimUrns.BULK_REQUEST)
        deserialized.failOnErrors shouldBe 1
        deserialized.operations.size shouldBe 2
        deserialized.operations[0].method shouldBe "POST"
        deserialized.operations[0].bulkId shouldBe bulkId
    }

    @Test
    fun `should serialize and deserialize BulkResponse`() {
        val bulkId = java.util.UUID.randomUUID().toString()
        val userId = java.util.UUID.randomUUID().toString()
        val response = BulkResponse(
            operations = listOf(
                BulkOperationResponse(
                    method = "POST",
                    bulkId = bulkId,
                    status = "201",
                    location = "https://example.com/v2/Users/$userId"
                ),
                BulkOperationResponse(method = "DELETE", status = "204")
            )
        )

        val json = mapper.writeValueAsString(response)
        val deserialized = mapper.readValue<BulkResponse>(json)

        deserialized.schemas shouldBe listOf(ScimUrns.BULK_RESPONSE)
        deserialized.operations.size shouldBe 2
        deserialized.operations[0].status shouldBe "201"
        deserialized.operations[0].location shouldBe "https://example.com/v2/Users/$userId"
    }
}
