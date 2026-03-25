package com.marcosbarbero.scim2.core.domain.model.patch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PatchModelsTest {

    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize PatchRequest`() {
        val request = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "displayName", value = "Babs"),
                PatchOperation(op = PatchOp.REMOVE, path = "title"),
                PatchOperation(op = PatchOp.REPLACE, path = "userName", value = "bjensen")
            )
        )

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue<PatchRequest>(json)

        deserialized.schemas shouldBe listOf("urn:ietf:params:scim:api:messages:2.0:PatchOp")
        deserialized.operations.size shouldBe 3
        deserialized.operations[0].op shouldBe PatchOp.ADD
        deserialized.operations[1].op shouldBe PatchOp.REMOVE
        deserialized.operations[2].op shouldBe PatchOp.REPLACE
    }

    @Test
    fun `PatchOp should serialize to lowercase`() {
        val json = mapper.writeValueAsString(PatchOp.ADD)
        json shouldBe "\"add\""
    }

    @Test
    fun `PatchOp should deserialize from lowercase`() {
        val op = mapper.readValue<PatchOp>("\"replace\"")
        op shouldBe PatchOp.REPLACE
    }
}
