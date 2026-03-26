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
import com.marcosbarbero.scim2.server.http.ScimHttpRequest
import com.marcosbarbero.scim2.server.http.ScimHttpResponse
import com.marcosbarbero.scim2.server.port.ScimRequestContext

interface ScimInterceptor {

    val order: Int get() = 0

    fun preHandle(request: ScimHttpRequest, context: ScimRequestContext): ScimHttpRequest = request

    fun postHandle(
        request: ScimHttpRequest,
        response: ScimHttpResponse,
        context: ScimRequestContext,
    ): ScimHttpResponse = response

    fun onError(
        request: ScimHttpRequest,
        exception: ScimException,
        context: ScimRequestContext,
    ): ScimException = exception
}
