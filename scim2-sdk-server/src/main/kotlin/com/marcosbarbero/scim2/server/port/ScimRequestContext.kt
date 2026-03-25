package com.marcosbarbero.scim2.server.port

data class ScimRequestContext(
    val principalId: String? = null,
    val roles: Set<String> = emptySet(),
    val attributes: Map<String, String> = emptyMap(),
    val requestedAttributes: List<String> = emptyList(),
    val excludedAttributes: List<String> = emptyList(),
    val correlationId: String = java.util.UUID.randomUUID().toString()
)
