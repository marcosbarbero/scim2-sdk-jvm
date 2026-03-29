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
package com.marcosbarbero.scim2.spring.observability

import com.marcosbarbero.scim2.core.observability.ScimTracer
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer

class OpenTelemetryScimTracer(private val tracer: Tracer) : ScimTracer {

    override fun <T> trace(operationName: String, attributes: Map<String, String>, block: () -> T): T {
        val span = tracer.spanBuilder(operationName).startSpan()
        attributes.forEach { (k, v) -> span.setAttribute(k, v) }
        val scope = span.makeCurrent()
        return try {
            block()
        } catch (e: Exception) {
            span.setStatus(StatusCode.ERROR, e.message ?: "")
            span.recordException(e)
            throw e
        } finally {
            scope.close()
            span.end()
        }
    }

    override fun currentCorrelationId(): String? {
        val spanContext = Span.current().spanContext
        return if (spanContext.isValid) spanContext.traceId else null
    }
}
