package com.marcosbarbero.scim2.server.engine

import com.marcosbarbero.scim2.core.domain.model.common.Meta
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.vo.ETag
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ETagEngineTest {

    private val faker = Faker()
    private val engine = ETagEngine()

    @Test
    fun `generateETag should return meta version when present`() {
        val etag = ETag("W/\"abc123\"")
        val user = User(
            userName = faker.name.firstName(),
            meta = Meta(version = etag)
        )

        val result = engine.generateETag(user)

        result shouldBe etag
    }

    @Test
    fun `generateETag should generate weak ETag from hash when no meta version`() {
        val user = User(userName = faker.name.firstName())

        val result = engine.generateETag(user)

        result.value shouldStartWith "W/\""
    }

    @Test
    fun `generateETag should produce different ETags for different resources`() {
        val user1 = User(userName = faker.name.firstName())
        val user2 = User(userName = faker.name.firstName() + "-other")

        val etag1 = engine.generateETag(user1)
        val etag2 = engine.generateETag(user2)

        etag1 shouldNotBe etag2
    }

    @Test
    fun `extractIfMatch should return null when header missing`() {
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users/123")

        val result = engine.extractIfMatch(request)

        result shouldBe null
    }

    @Test
    fun `extractIfMatch should return ETag from header`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "/Users/123",
            headers = mapOf("If-Match" to listOf("W/\"abc\""))
        )

        val result = engine.extractIfMatch(request)

        result shouldBe ETag("W/\"abc\"")
    }

    @Test
    fun `extractIfNoneMatch should return ETag from header`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "/Users/123",
            headers = mapOf("If-None-Match" to listOf("W/\"xyz\""))
        )

        val result = engine.extractIfNoneMatch(request)

        result shouldBe ETag("W/\"xyz\"")
    }

    @Test
    fun `checkPrecondition should return true when required is null`() {
        val result = engine.checkPrecondition(ETag("W/\"abc\""), null)

        result shouldBe true
    }

    @Test
    fun `checkPrecondition should return true when current is null`() {
        val result = engine.checkPrecondition(null, ETag("W/\"abc\""))

        result shouldBe true
    }

    @Test
    fun `checkPrecondition should return true when ETags match`() {
        val etag = ETag("W/\"abc\"")

        val result = engine.checkPrecondition(etag, etag)

        result shouldBe true
    }

    @Test
    fun `checkPrecondition should return false when ETags mismatch`() {
        val result = engine.checkPrecondition(ETag("W/\"abc\""), ETag("W/\"xyz\""))

        result shouldBe false
    }

    @Test
    fun `checkPreconditionOrThrow should throw when ETags mismatch`() {
        assertThrows<PreconditionFailedException> {
            engine.checkPreconditionOrThrow(ETag("W/\"abc\""), ETag("W/\"xyz\""))
        }
    }

    @Test
    fun `checkPreconditionOrThrow should not throw when ETags match`() {
        engine.checkPreconditionOrThrow(ETag("W/\"abc\""), ETag("W/\"abc\""))
    }
}
