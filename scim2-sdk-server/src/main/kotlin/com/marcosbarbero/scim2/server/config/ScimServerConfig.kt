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
package com.marcosbarbero.scim2.server.config

data class ScimServerConfig(
    val basePath: String = "/scim/v2",
    val bulkEnabled: Boolean = true,
    val bulkMaxOperations: Int = 1000,
    val bulkMaxPayloadSize: Long = 1_048_576,
    val filterEnabled: Boolean = true,
    val filterMaxResults: Int = 200,
    val sortEnabled: Boolean = false,
    val etagEnabled: Boolean = true,
    val changePasswordEnabled: Boolean = false,
    val patchEnabled: Boolean = true,
    val defaultPageSize: Int = 100,
    val maxPageSize: Int = 1000
)
