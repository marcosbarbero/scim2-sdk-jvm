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
