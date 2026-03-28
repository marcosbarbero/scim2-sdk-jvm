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
package com.marcosbarbero.scim2.server.provisioning

import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource

/**
 * Abstraction for pushing SCIM resources to an outbound target.
 *
 * This is framework-agnostic — implement using [com.marcosbarbero.scim2.client.api.ScimClient],
 * plain HTTP, or any other transport.
 *
 * ```java
 * // Example with ScimClient
 * ScimOutboundTarget target = new ScimOutboundTarget() {
 *     public void create(String endpoint, ScimResource resource) {
 *         scimClient.create(endpoint, resource, resource.getClass().kotlin);
 *     }
 *     public void replace(String endpoint, String id, ScimResource resource) {
 *         scimClient.replace(endpoint, id, resource, resource.getClass().kotlin);
 *     }
 *     public void delete(String endpoint, String id) {
 *         scimClient.delete(endpoint, id);
 *     }
 * };
 * ```
 */
interface ScimOutboundTarget {
    fun create(endpoint: String, resource: ScimResource)
    fun replace(endpoint: String, id: String, resource: ScimResource)
    fun delete(endpoint: String, id: String)
}
