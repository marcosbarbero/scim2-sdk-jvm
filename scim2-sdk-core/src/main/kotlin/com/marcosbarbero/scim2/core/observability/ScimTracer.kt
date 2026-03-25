package com.marcosbarbero.scim2.core.observability

interface ScimTracer {
    fun <T> trace(operationName: String, attributes: Map<String, String> = emptyMap(), block: () -> T): T
    fun currentCorrelationId(): String?
}

object NoOpScimTracer : ScimTracer {
    override fun <T> trace(operationName: String, attributes: Map<String, String>, block: () -> T): T = block()
    override fun currentCorrelationId(): String? = null
}
