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
package com.marcosbarbero.scim2.spring.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "scim_resources")
class ScimResourceEntity(
    @Id
    @Column(name = "id", length = 255)
    open var id: String = "",

    @Column(name = "resource_type", nullable = false, length = 100)
    open var resourceType: String = "",

    @Column(name = "external_id", length = 255)
    open var externalId: String? = null,

    @Column(name = "display_name", length = 500)
    open var displayName: String? = null,

    @Column(name = "resource_json", nullable = false, columnDefinition = "TEXT")
    open var resourceJson: String = "",

    @Column(name = "version", nullable = false)
    open var version: Long = 1,

    @Column(name = "created", nullable = false)
    open var created: Instant = Instant.now(),

    @Column(name = "last_modified", nullable = false)
    open var lastModified: Instant = Instant.now()
)
