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
package com.marcosbarbero.scim2.core.domain

/**
 * SCIM 2.0 standard URN constants as defined in RFC 7643 and RFC 7644.
 */
object ScimUrns {
    // Core resource schemas (RFC 7643)
    const val USER: String = "urn:ietf:params:scim:schemas:core:2.0:User"
    const val GROUP: String = "urn:ietf:params:scim:schemas:core:2.0:Group"

    // Extension schemas (RFC 7643)
    const val ENTERPRISE_USER: String = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"

    // Discovery schemas (RFC 7643)
    const val SERVICE_PROVIDER_CONFIG: String = "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"
    const val RESOURCE_TYPE: String = "urn:ietf:params:scim:schemas:core:2.0:ResourceType"
    const val SCHEMA: String = "urn:ietf:params:scim:schemas:core:2.0:Schema"

    // Protocol message schemas (RFC 7644)
    const val ERROR: String = "urn:ietf:params:scim:api:messages:2.0:Error"
    const val LIST_RESPONSE: String = "urn:ietf:params:scim:api:messages:2.0:ListResponse"
    const val SEARCH_REQUEST: String = "urn:ietf:params:scim:api:messages:2.0:SearchRequest"
    const val PATCH_OP: String = "urn:ietf:params:scim:api:messages:2.0:PatchOp"
    const val BULK_REQUEST: String = "urn:ietf:params:scim:api:messages:2.0:BulkRequest"
    const val BULK_RESPONSE: String = "urn:ietf:params:scim:api:messages:2.0:BulkResponse"
}
