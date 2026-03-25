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
package com.marcosbarbero.scim2.client.api

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import kotlin.reflect.full.findAnnotation

// ===== User Operations =====

fun ScimClient.createUser(user: User): ScimResponse<User> =
    create("/Users", user, User::class)

fun ScimClient.getUser(id: String): ScimResponse<User> =
    get("/Users", id, User::class)

fun ScimClient.replaceUser(id: String, user: User): ScimResponse<User> =
    replace("/Users", id, user, User::class)

fun ScimClient.patchUser(id: String, patchRequest: PatchRequest): ScimResponse<User> =
    patch("/Users", id, patchRequest, User::class)

fun ScimClient.deleteUser(id: String): Unit =
    delete("/Users", id)

fun ScimClient.searchUsers(request: SearchRequest = SearchRequest()): ScimResponse<ListResponse<User>> =
    search("/Users", request, User::class)

fun ScimClient.searchUsers(filter: String): ScimResponse<ListResponse<User>> =
    search("/Users", SearchRequest(filter = filter), User::class)

// ===== Group Operations =====

fun ScimClient.createGroup(group: Group): ScimResponse<Group> =
    create("/Groups", group, Group::class)

fun ScimClient.getGroup(id: String): ScimResponse<Group> =
    get("/Groups", id, Group::class)

fun ScimClient.replaceGroup(id: String, group: Group): ScimResponse<Group> =
    replace("/Groups", id, group, Group::class)

fun ScimClient.patchGroup(id: String, patchRequest: PatchRequest): ScimResponse<Group> =
    patch("/Groups", id, patchRequest, Group::class)

fun ScimClient.deleteGroup(id: String): Unit =
    delete("/Groups", id)

fun ScimClient.searchGroups(request: SearchRequest = SearchRequest()): ScimResponse<ListResponse<Group>> =
    search("/Groups", request, Group::class)

// ===== Generic typed operations (reads endpoint from @ScimResource annotation) =====

inline fun <reified T : ScimResource> ScimClient.createResource(resource: T): ScimResponse<T> {
    val endpoint = T::class.findAnnotation<com.marcosbarbero.scim2.core.schema.annotation.ScimResource>()?.endpoint
        ?: throw IllegalArgumentException("${T::class.simpleName} is not annotated with @ScimResource")
    return create(endpoint, resource, T::class)
}

inline fun <reified T : ScimResource> ScimClient.getResource(id: String): ScimResponse<T> {
    val endpoint = T::class.findAnnotation<com.marcosbarbero.scim2.core.schema.annotation.ScimResource>()?.endpoint
        ?: throw IllegalArgumentException("${T::class.simpleName} is not annotated with @ScimResource")
    return get(endpoint, id, T::class)
}

inline fun <reified T : ScimResource> ScimClient.searchResources(
    request: SearchRequest = SearchRequest()
): ScimResponse<ListResponse<T>> {
    val endpoint = T::class.findAnnotation<com.marcosbarbero.scim2.core.schema.annotation.ScimResource>()?.endpoint
        ?: throw IllegalArgumentException("${T::class.simpleName} is not annotated with @ScimResource")
    return search(endpoint, request, T::class)
}
