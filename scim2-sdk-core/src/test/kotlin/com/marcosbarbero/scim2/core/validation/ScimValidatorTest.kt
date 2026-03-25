package com.marcosbarbero.scim2.core.validation

import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ScimValidatorTest {

    private val validator = ScimValidator()

    @Nested
    inner class ValidateForCreateTest {

        @Test
        fun `should pass for valid User`() {
            val user = User(userName = "bjensen")
            val errors = validator.validateForCreate(user)
            errors.shouldBeEmpty()
        }

        @Test
        fun `should fail when required userName is blank`() {
            val user = User(userName = "")
            val errors = validator.validateForCreate(user)
            errors.shouldNotBeEmpty()
            errors[0] shouldContain "userName"
        }

        @Test
        fun `should detect readOnly attributes set by client`() {
            // id is readOnly per spec - setting it in a create should produce a warning
            val user = User(id = "client-set-id", userName = "bjensen")
            val errors = validator.validateForCreate(user)
            // ReadOnly attributes in create are typically ignored, not errors
            // But we flag them for awareness
            errors.any { it.contains("id") }.shouldBeTrue()
        }
    }

    @Nested
    inner class ValidateForReplaceTest {

        @Test
        fun `should pass for valid replacement`() {
            val existing = User(id = "123", userName = "bjensen")
            val replacement = User(id = "123", userName = "bjensen", displayName = "Babs")
            val errors = validator.validateForReplace(existing, replacement)
            errors.shouldBeEmpty()
        }

        @Test
        fun `should fail when required attribute missing in replacement`() {
            val existing = User(id = "123", userName = "bjensen")
            val replacement = User(id = "123", userName = "")
            val errors = validator.validateForReplace(existing, replacement)
            errors.shouldNotBeEmpty()
        }
    }

    @Nested
    inner class ValidatePatchTest {

        @Test
        fun `should pass for valid patch`() {
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "displayName", value = "Babs")
            ))
            val errors = validator.validatePatch(request)
            errors.shouldBeEmpty()
        }

        @Test
        fun `should fail when patch has no operations`() {
            val request = PatchRequest(operations = emptyList())
            val errors = validator.validatePatch(request)
            errors.shouldNotBeEmpty()
            errors[0] shouldContain "operation"
        }

        @Test
        fun `should fail when remove operation has no path`() {
            val request = PatchRequest(operations = listOf(
                PatchOperation(op = PatchOp.REMOVE)
            ))
            val errors = validator.validatePatch(request)
            errors.shouldNotBeEmpty()
            errors[0] shouldContain "path"
        }
    }
}
