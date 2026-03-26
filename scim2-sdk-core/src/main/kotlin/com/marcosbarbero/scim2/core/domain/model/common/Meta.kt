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
package com.marcosbarbero.scim2.core.domain.model.common

import com.marcosbarbero.scim2.core.domain.vo.ETag
import java.net.URI
import java.time.Instant

data class Meta @JvmOverloads constructor(
    val resourceType: String? = null,
    val created: Instant? = null,
    val lastModified: Instant? = null,
    val location: URI? = null,
    val version: ETag? = null,
)
