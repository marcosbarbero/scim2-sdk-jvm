package com.marcosbarbero.scim2.server.http

data class ScimHttpRequest(
    val method: HttpMethod,
    val path: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val queryParameters: Map<String, List<String>> = emptyMap(),
    val body: ByteArray? = null
) {
    fun header(name: String): String? =
        headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()

    fun queryParam(name: String): String? =
        queryParameters[name]?.firstOrNull()

    fun queryParamList(name: String): List<String> =
        queryParameters[name] ?: emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScimHttpRequest) return false
        return method == other.method &&
            path == other.path &&
            headers == other.headers &&
            queryParameters == other.queryParameters &&
            body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + queryParameters.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}

enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE
}
