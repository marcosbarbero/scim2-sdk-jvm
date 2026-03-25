package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest

/**
 * Java-friendly static methods for type-safe SCIM operations.
 * Kotlin users should prefer the extension functions on [ScimClient].
 */
object ScimClients {

    // ===== User Operations =====

    @JvmStatic
    fun createUser(client: ScimClient, user: User): ScimResponse<User> =
        client.createUser(user)

    @JvmStatic
    fun getUser(client: ScimClient, id: String): ScimResponse<User> =
        client.getUser(id)

    @JvmStatic
    fun replaceUser(client: ScimClient, id: String, user: User): ScimResponse<User> =
        client.replaceUser(id, user)

    @JvmStatic
    fun patchUser(client: ScimClient, id: String, patchRequest: PatchRequest): ScimResponse<User> =
        client.patchUser(id, patchRequest)

    @JvmStatic
    fun deleteUser(client: ScimClient, id: String): Unit =
        client.deleteUser(id)

    @JvmStatic
    fun searchUsers(client: ScimClient, request: SearchRequest): ScimResponse<ListResponse<User>> =
        client.searchUsers(request)

    @JvmStatic
    fun searchUsers(client: ScimClient, filter: String): ScimResponse<ListResponse<User>> =
        client.searchUsers(filter)

    // ===== Group Operations =====

    @JvmStatic
    fun createGroup(client: ScimClient, group: Group): ScimResponse<Group> =
        client.createGroup(group)

    @JvmStatic
    fun getGroup(client: ScimClient, id: String): ScimResponse<Group> =
        client.getGroup(id)

    @JvmStatic
    fun replaceGroup(client: ScimClient, id: String, group: Group): ScimResponse<Group> =
        client.replaceGroup(id, group)

    @JvmStatic
    fun patchGroup(client: ScimClient, id: String, patchRequest: PatchRequest): ScimResponse<Group> =
        client.patchGroup(id, patchRequest)

    @JvmStatic
    fun deleteGroup(client: ScimClient, id: String): Unit =
        client.deleteGroup(id)

    @JvmStatic
    fun searchGroups(client: ScimClient, request: SearchRequest): ScimResponse<ListResponse<Group>> =
        client.searchGroups(request)
}
