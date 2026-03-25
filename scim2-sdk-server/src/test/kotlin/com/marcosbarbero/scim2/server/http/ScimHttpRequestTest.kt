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

class ScimHttpRequestTest {

    @Test
    fun `header should be case-insensitive`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "/Users",
            headers = mapOf("Content-Type" to listOf("application/scim+json"))
        )

        request.header("content-type") shouldBe "application/scim+json"
        request.header("CONTENT-TYPE") shouldBe "application/scim+json"
    }

    @Test
    fun `header should return null when absent`() {
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")

        request.header("X-Missing").shouldBeNull()
    }

    @Test
    fun `queryParam should return first value`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "/Users",
            queryParameters = mapOf("filter" to listOf("userName eq \"bjensen\""))
        )

        request.queryParam("filter") shouldBe "userName eq \"bjensen\""
    }

    @Test
    fun `queryParam should return null when absent`() {
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")

        request.queryParam("filter").shouldBeNull()
    }

    @Test
    fun `queryParamList should return all values`() {
        val request = ScimHttpRequest(
            method = HttpMethod.GET,
            path = "/Users",
            queryParameters = mapOf("schemas" to listOf("a", "b"))
        )

        request.queryParamList("schemas") shouldBe listOf("a", "b")
    }

    @Test
    fun `queryParamList should return empty for absent param`() {
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")

        request.queryParamList("missing") shouldBe emptyList()
    }

    @Test
    fun `equality should include body`() {
        val r1 = ScimHttpRequest(method = HttpMethod.POST, path = "/Users", body = "a".toByteArray())
        val r2 = ScimHttpRequest(method = HttpMethod.POST, path = "/Users", body = "a".toByteArray())
        val r3 = ScimHttpRequest(method = HttpMethod.POST, path = "/Users", body = "b".toByteArray())

        (r1 == r2) shouldBe true
        (r1 == r3) shouldBe false
    }
}
