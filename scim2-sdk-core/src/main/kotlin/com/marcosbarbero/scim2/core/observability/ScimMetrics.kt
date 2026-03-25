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
    fun recordOperation(endpoint: String, method: String, status: Int, duration: Duration)
    fun recordFilterParse(duration: Duration, success: Boolean)
    fun recordPatchOperations(endpoint: String, operationCount: Int, duration: Duration)
    fun recordBulkOperation(operationCount: Int, failureCount: Int, duration: Duration)
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
