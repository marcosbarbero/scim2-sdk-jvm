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
package com.marcosbarbero.scim2.core.domain.model.patch

import com.marcosbarbero.scim2.core.domain.ScimUrns
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PatchModelsTest {

    private val faker = Faker()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should serialize and deserialize PatchRequest`() {
        val displayName = faker.name.name()
        val userName = faker.name.firstName().lowercase()
        val request = PatchRequest(
            operations = listOf(
                PatchOperation(op = PatchOp.ADD, path = "displayName", value = mapper.valueToTree(displayName)),
                PatchOperation(op = PatchOp.REMOVE, path = "title"),
                PatchOperation(op = PatchOp.REPLACE, path = "userName", value = mapper.valueToTree(userName))
            )
        )

        val json = mapper.writeValueAsString(request)
        val deserialized = mapper.readValue<PatchRequest>(json)

        deserialized.schemas shouldBe listOf(ScimUrns.PATCH_OP)
        deserialized.operations.size shouldBe 3
        deserialized.operations[0].op shouldBe PatchOp.ADD
        deserialized.operations[1].op shouldBe PatchOp.REMOVE
        deserialized.operations[2].op shouldBe PatchOp.REPLACE
    }

    @Test
    fun `PatchOp should serialize to lowercase`() {
        val json = mapper.writeValueAsString(PatchOp.ADD)
        json shouldBe "\"add\""
    }

    @Test
    fun `PatchOp should deserialize from lowercase`() {
        val op = mapper.readValue<PatchOp>("\"replace\"")
        op shouldBe PatchOp.REPLACE
    }
}
