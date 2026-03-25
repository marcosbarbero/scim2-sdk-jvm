package com.marcosbarbero.scim2.client.dsl

import tools.jackson.databind.node.StringNode
import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PatchDslTest {

    @Test
    fun `scimPatch with add and remove produces correct PatchRequest`() {
        val request = scimPatch {
            add("displayName", "John")
            remove("title")
        }
        request.schemas shouldBe listOf(ScimUrns.PATCH_OP)
        request.operations shouldHaveSize 2
        request.operations[0].op shouldBe PatchOp.ADD
        request.operations[0].path shouldBe "displayName"
        request.operations[0].value shouldBe StringNode("John")
        request.operations[1].op shouldBe PatchOp.REMOVE
        request.operations[1].path shouldBe "title"
        request.operations[1].value shouldBe null
    }

    @Test
    fun `scimPatch with replace produces correct PatchRequest`() {
        val request = scimPatch { replace("userName", "newUser") }
        request.operations shouldHaveSize 1
        request.operations[0].op shouldBe PatchOp.REPLACE
        request.operations[0].path shouldBe "userName"
        request.operations[0].value shouldBe StringNode("newUser")
    }

    @Test
    fun `scimPatch with JsonNode add`() {
        val jsonNode = StringNode("jsonValue")
        val request = scimPatch { add("field", jsonNode) }
        request.operations shouldHaveSize 1
        request.operations[0].op shouldBe PatchOp.ADD
        request.operations[0].value shouldBe jsonNode
    }

    @Test
    fun `scimPatch with no path on add`() {
        val jsonNode = StringNode("value")
        val request = scimPatch { add(value = jsonNode) }
        request.operations shouldHaveSize 1
        request.operations[0].op shouldBe PatchOp.ADD
        request.operations[0].path shouldBe null
    }

    @Test
    fun `scimPatch with empty block produces empty operations`() {
        val request = scimPatch { }
        request.operations shouldHaveSize 0
        request.schemas shouldBe listOf(ScimUrns.PATCH_OP)
    }
}
