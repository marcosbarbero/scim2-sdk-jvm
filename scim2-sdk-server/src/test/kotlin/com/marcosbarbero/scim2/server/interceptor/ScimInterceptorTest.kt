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
package com.marcosbarbero.scim2.server.interceptor

import com.marcosbarbero.scim2.core.domain.model.error.ScimException
import com.marcosbarbero.scim2.server.http.HttpMethod
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ScimInterceptorTest {

    private val context = ScimRequestContext()

    @Test
    fun `default preHandle should return request unchanged`() {
        val interceptor = object : ScimInterceptor {}
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")

        val result = interceptor.preHandle(request, context)

        result shouldBe request
    }

    @Test
    fun `default postHandle should return response unchanged`() {
        val interceptor = object : ScimInterceptor {}
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")
        val response = ScimHttpResponse.ok(byteArrayOf(1, 2, 3))

        val result = interceptor.postHandle(request, response, context)

        result shouldBe response
    }

    @Test
    fun `default onError should return exception unchanged`() {
        val interceptor = object : ScimInterceptor {}
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")
        val exception = ScimException(status = 404, detail = "Not found")

        val result = interceptor.onError(request, exception, context)

        result shouldBe exception
    }

    @Test
    fun `default order should be 0`() {
        val interceptor = object : ScimInterceptor {}

        interceptor.order shouldBe 0
    }

    @Test
    fun `preHandle can modify request`() {
        val interceptor = object : ScimInterceptor {
            override fun preHandle(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpRequest =
                request.copy(headers = request.headers + ("X-Custom" to listOf("value")))
        }
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")

        val result = interceptor.preHandle(request, context)

        result.headers["X-Custom"] shouldBe listOf("value")
    }

    @Test
    fun `postHandle can modify response`() {
        val interceptor = object : ScimInterceptor {
            override fun postHandle(
                request: ScimHttpRequest,
                response: ScimHttpResponse,
                context: ScimRequestContext
            ): ScimHttpResponse =
                response.copy(headers = response.headers + ("X-Custom" to "value"))
        }
        val request = ScimHttpRequest(method = HttpMethod.GET, path = "/Users")
        val response = ScimHttpResponse.ok(byteArrayOf())

        val result = interceptor.postHandle(request, response, context)

        result.headers["X-Custom"] shouldBe "value"
    }

    @Test
    fun `interceptors should be sortable by order`() {
        val first = object : ScimInterceptor {
            override val order: Int = 1
        }
        val second = object : ScimInterceptor {
            override val order: Int = 2
        }
        val zero = object : ScimInterceptor {}

        val sorted = listOf(second, zero, first).sortedBy { it.order }

        sorted[0].order shouldBe 0
        sorted[1].order shouldBe 1
        sorted[2].order shouldBe 2
    }
}
