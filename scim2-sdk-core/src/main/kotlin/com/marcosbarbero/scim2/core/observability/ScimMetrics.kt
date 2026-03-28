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

import java.time.Duration

interface ScimMetrics {
    /**
     * Records the total duration of a SCIM request (scim.request.duration).
     * This includes the full request lifecycle: interceptors, handler execution,
     * serialization, and meta-enrichment.
     *
     * Compare with [recordSearchResults] and [recordPatchOperations] which measure
     * only the handler execution time.
     */
    fun recordOperation(endpoint: String, method: String, status: Int, duration: Duration)

    /**
     * Records the duration of a SCIM filter expression parse.
     *
     * NOTE: This metric is not currently instrumented by the dispatcher because filter parsing
     * occurs inside [com.marcosbarbero.scim2.core.filter.FilterEngine] (core module), which does
     * not depend on ScimMetrics. Custom repository implementations that parse filters directly
     * can call this method to record parse timings.
     *
     * TODO: Consider a FilterEngine decorator or callback to instrument this automatically.
     */
    fun recordFilterParse(duration: Duration, success: Boolean)

    /**
     * Records patch handler execution time (scim.patch.duration) and operation count.
     * The [duration] measures only the handler's patch execution, excluding request
     * deserialization, ETag handling, response serialization, and meta enrichment.
     */
    fun recordPatchOperations(endpoint: String, operationCount: Int, duration: Duration)
    fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: Duration)

    /**
     * Records search handler execution time (scim.search.duration) and result count.
     * The [duration] measures only the handler's search execution, excluding request
     * deserialization, pagination defaults, response serialization, and meta enrichment.
     */
    fun recordSearchResults(endpoint: String, totalResults: Int, duration: Duration)
    fun incrementActiveRequests(endpoint: String)
    fun decrementActiveRequests(endpoint: String)
}

object NoOpScimMetrics : ScimMetrics {
    override fun recordOperation(endpoint: String, method: String, status: Int, duration: Duration) {}
    override fun recordFilterParse(duration: Duration, success: Boolean) {}
    override fun recordPatchOperations(endpoint: String, operationCount: Int, duration: Duration) {}
    override fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: Duration) {}
    override fun recordSearchResults(endpoint: String, totalResults: Int, duration: Duration) {}
    override fun incrementActiveRequests(endpoint: String) {}
    override fun decrementActiveRequests(endpoint: String) {}
}
