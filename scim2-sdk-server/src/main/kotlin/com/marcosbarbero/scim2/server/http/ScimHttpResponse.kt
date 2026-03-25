package com.marcosbarbero.scim2.server.http

data class ScimHttpResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScimHttpResponse) return false
        return status == other.status &&
            headers == other.headers &&
            body.contentEquals(other.body)
    }

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        fun ok(body: ByteArray, headers: Map<String, String> = emptyMap()): ScimHttpResponse =
            ScimHttpResponse(status = 200, headers = headers + SCIM_CONTENT_TYPE, body = body)

        fun created(body: ByteArray, location: String): ScimHttpResponse =
            ScimHttpResponse(
                status = 201,
                headers = mapOf("Location" to location) + SCIM_CONTENT_TYPE,
                body = body
            )

        fun noContent(): ScimHttpResponse =
            ScimHttpResponse(status = 204)

        fun error(status: Int, body: ByteArray): ScimHttpResponse =
            ScimHttpResponse(status = status, headers = SCIM_CONTENT_TYPE, body = body)

        private val SCIM_CONTENT_TYPE = mapOf("Content-Type" to "application/scim+json")
    }
}
