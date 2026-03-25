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
package com.marcosbarbero.scim2.core.observability

interface ScimTracer {
    fun <T> trace(operationName: String, attributes: Map<String, String> = emptyMap(), block: () -> T): T
    fun currentCorrelationId(): String?
}

object NoOpScimTracer : ScimTracer {
    override fun <T> trace(operationName: String, attributes: Map<String, String>, block: () -> T): T = block()
    override fun currentCorrelationId(): String? = null
}
