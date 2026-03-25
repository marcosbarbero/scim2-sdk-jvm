package com.marcosbarbero.scim2.server.port

interface AuthorizationEvaluator {

    fun canCreate(resourceType: String, context: ScimRequestContext): Boolean = true

    fun canRead(resourceType: String, resourceId: String, context: ScimRequestContext): Boolean = true

    fun canUpdate(resourceType: String, resourceId: String, context: ScimRequestContext): Boolean = true

    fun canDelete(resourceType: String, resourceId: String, context: ScimRequestContext): Boolean = true

    fun canSearch(resourceType: String, context: ScimRequestContext): Boolean = true

    fun canBulk(context: ScimRequestContext): Boolean = true
}
