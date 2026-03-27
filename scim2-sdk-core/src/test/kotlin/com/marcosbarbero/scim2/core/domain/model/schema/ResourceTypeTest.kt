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
package com.marcosbarbero.scim2.core.domain.model.schema

import com.marcosbarbero.scim2.core.domain.ScimUrns
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResourceTypeTest {

    @Test
    fun `should include schemas attribute with default ResourceType URN`() {
        val rt = ResourceType(
            id = "User",
            name = "User",
            description = "User Account",
            endpoint = "/Users",
            schema = ScimUrns.USER,
        )

        rt.schemas shouldContainExactly listOf(ScimUrns.RESOURCE_TYPE)
    }

    @Test
    fun `should allow custom schemas attribute`() {
        val customSchemas = listOf(ScimUrns.RESOURCE_TYPE, "urn:custom:extension")
        val rt = ResourceType(
            schemas = customSchemas,
            id = "User",
            name = "User",
            description = null,
            endpoint = "/Users",
            schema = ScimUrns.USER,
        )

        rt.schemas shouldBe customSchemas
    }
}
