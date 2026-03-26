/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marcosbarbero.scim2.server.http

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ScimHttpResponseTest {

    @Test
    fun `ok should set status 200 and SCIM content type`() {
        val response = ScimHttpResponse.ok("{}".toByteArray())

        response.status shouldBe 200
        response.headers["Content-Type"] shouldBe "application/scim+json"
    }

    @Test
    fun `ok should include extra headers`() {
        val response = ScimHttpResponse.ok("{}".toByteArray(), mapOf("ETag" to "W/\"abc\""))

        response.headers["ETag"] shouldBe "W/\"abc\""
        response.headers["Content-Type"] shouldBe "application/scim+json"
    }

    @Test
    fun `created should set status 201 and Location header`() {
        val response = ScimHttpResponse.created("{}".toByteArray(), "/Users/123")

        response.status shouldBe 201
        response.headers["Location"] shouldBe "/Users/123"
        response.headers["Content-Type"] shouldBe "application/scim+json"
    }

    @Test
    fun `noContent should set status 204 with no body`() {
        val response = ScimHttpResponse.noContent()

        response.status shouldBe 204
        response.body.shouldBeNull()
    }

    @Test
    fun `error should set status and SCIM content type`() {
        val response = ScimHttpResponse.error(400, "{}".toByteArray())

        response.status shouldBe 400
        response.headers["Content-Type"] shouldBe "application/scim+json"
    }

    @Test
    fun `error with custom headers should use provided headers`() {
        val response = ScimHttpResponse.error(500, "{}".toByteArray(), mapOf("Content-Type" to "application/problem+json"))

        response.status shouldBe 500
        response.headers["Content-Type"] shouldBe "application/problem+json"
    }

    @Test
    fun `equals should return true for same status and headers and body`() {
        val a = ScimHttpResponse(status = 200, headers = mapOf("A" to "B"), body = "hello".toByteArray())
        val b = ScimHttpResponse(status = 200, headers = mapOf("A" to "B"), body = "hello".toByteArray())

        (a == b) shouldBe true
    }

    @Test
    fun `equals should return false for different status`() {
        val a = ScimHttpResponse(status = 200)
        val b = ScimHttpResponse(status = 404)

        (a == b) shouldBe false
    }

    @Test
    fun `equals should return false for different body`() {
        val a = ScimHttpResponse(status = 200, body = "a".toByteArray())
        val b = ScimHttpResponse(status = 200, body = "b".toByteArray())

        (a == b) shouldBe false
    }

    @Test
    fun `equals should return false for non-ScimHttpResponse`() {
        val a = ScimHttpResponse(status = 200)

        (a.equals("not a response")) shouldBe false
    }

    @Test
    fun `equals should return true for same reference`() {
        val a = ScimHttpResponse(status = 200)

        (a == a) shouldBe true
    }

    @Test
    fun `hashCode should be consistent for equal objects`() {
        val a = ScimHttpResponse(status = 200, headers = mapOf("A" to "B"), body = "hello".toByteArray())
        val b = ScimHttpResponse(status = 200, headers = mapOf("A" to "B"), body = "hello".toByteArray())

        a.hashCode() shouldBe b.hashCode()
    }

    @Test
    fun `hashCode should handle null body`() {
        val response = ScimHttpResponse(status = 204)
        // should not throw
        response.hashCode() shouldBe response.hashCode()
    }
}
