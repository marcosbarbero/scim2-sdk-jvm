package com.marcosbarbero.scim2.core.patch

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.marcosbarbero.scim2.core.domain.model.common.MultiValuedAttribute
import com.marcosbarbero.scim2.core.domain.model.error.InvalidPathException
import com.marcosbarbero.scim2.core.domain.model.error.InvalidValueException
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PatchEngineTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    private val engine = PatchEngine(objectMapper)

    private fun baseUser() = User(
        id = "123",
        userName = "bjensen",
        displayName = "Babs Jensen",
        emails = listOf(
            MultiValuedAttribute(value = "bjensen@example.com", type = "work", primary = true),
            MultiValuedAttribute(value = "babs@home.org", type = "home")
        )
    )

    @Nested
    inner class AddOperations {

        @Test
        fun `should add simple attribute`() {
            val user = User(userName = "bjensen")
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "displayName", value = "Babs Jensen")
            ))

            val result = engine.apply(user, request)
            result.displayName shouldBe "Babs Jensen"
        }

        @Test
        fun `should add to multi-valued attribute`() {
            val user = baseUser()
            val newEmail = mapOf("value" to "new@example.com", "type" to "other")
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "emails", value = listOf(newEmail))
            ))

            val result = engine.apply(user, request)
            result.emails shouldHaveSize 3
        }

        @Test
        fun `should add with no path by merging object`() {
            val user = User(userName = "bjensen")
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.ADD, value = mapOf("displayName" to "Babs", "title" to "Guide"))
            ))

            val result = engine.apply(user, request)
            result.displayName shouldBe "Babs"
            result.title shouldBe "Guide"
        }
    }

    @Nested
    inner class RemoveOperations {

        @Test
        fun `should remove simple attribute`() {
            val user = baseUser()
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.REMOVE, path = "displayName")
            ))

            val result = engine.apply(user, request)
            result.displayName.shouldBeNull()
        }

        @Test
        fun `should remove from multi-valued with filter`() {
            val user = baseUser()
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.REMOVE, path = "emails[type eq \"work\"]")
            ))

            val result = engine.apply(user, request)
            result.emails shouldHaveSize 1
            result.emails[0].type shouldBe "home"
        }

        @Test
        fun `should throw when remove has no path`() {
            val user = baseUser()
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.REMOVE)
            ))

            shouldThrow<InvalidPathException> {
                engine.apply(user, request)
            }
        }
    }

    @Nested
    inner class ReplaceOperations {

        @Test
        fun `should replace simple attribute`() {
            val user = baseUser()
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = "Barbara Jensen")
            ))

            val result = engine.apply(user, request)
            result.displayName shouldBe "Barbara Jensen"
        }

        @Test
        fun `should replace multi-valued attribute entirely`() {
            val user = baseUser()
            val newEmails = listOf(mapOf("value" to "only@example.com", "type" to "work"))
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.REPLACE, path = "emails", value = newEmails)
            ))

            val result = engine.apply(user, request)
            result.emails shouldHaveSize 1
            result.emails[0].value shouldBe "only@example.com"
        }
    }

    @Nested
    inner class MultipleOperations {

        @Test
        fun `should apply multiple operations in sequence`() {
            val user = User(userName = "bjensen")
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "displayName", value = "Babs"),
                PatchOperation(op = PatchOp.ADD, path = "title", value = "Guide"),
                PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = "Barbara")
            ))

            val result = engine.apply(user, request)
            result.displayName shouldBe "Barbara"
            result.title shouldBe "Guide"
        }
    }

    @Nested
    inner class PreservesIdentity {

        @Test
        fun `should preserve schemas, id, and userName`() {
            val user = baseUser()
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "title", value = "Guide")
            ))

            val result = engine.apply(user, request)
            result.id shouldBe "123"
            result.userName shouldBe "bjensen"
            result.schemas shouldBe user.schemas
        }
    }
}
