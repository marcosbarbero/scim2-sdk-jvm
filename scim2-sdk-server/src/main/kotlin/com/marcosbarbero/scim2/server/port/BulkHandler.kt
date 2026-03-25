package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.bulk.BulkRequest
import com.marcosbarbero.scim2.core.domain.model.bulk.BulkResponse

interface BulkHandler {

    fun processBulk(request: BulkRequest, context: ScimRequestContext): BulkResponse
}
