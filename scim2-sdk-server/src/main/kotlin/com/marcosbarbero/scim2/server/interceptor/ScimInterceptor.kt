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
        context: ScimRequestContext
    ): ScimHttpResponse = response

    fun onError(
        request: ScimHttpRequest,
        exception: ScimException,
        context: ScimRequestContext
    ): ScimException = exception
}
