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
package com.marcosbarbero.scim2.server.port

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest

interface ResourceHandler<T : ScimResource> {

    val resourceType: Class<T>

    val endpoint: String

    fun get(id: String, context: ScimRequestContext): T

    fun create(resource: T, context: ScimRequestContext): T

    fun replace(id: String, resource: T, version: String?, context: ScimRequestContext): T

    fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): T

    fun delete(id: String, version: String?, context: ScimRequestContext)

    fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<T>
}
