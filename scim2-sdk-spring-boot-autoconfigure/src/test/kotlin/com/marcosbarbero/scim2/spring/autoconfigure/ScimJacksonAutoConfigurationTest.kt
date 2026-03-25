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
package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimJacksonAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                ScimJacksonAutoConfiguration::class.java
            )
        )

    @Test
    fun `auto-configures ScimSerializer with Jackson`() {
        contextRunner.run { context ->
            val serializer = context.getBean(ScimSerializer::class.java)
            serializer.shouldNotBeNull()
            serializer.shouldBeInstanceOf<JacksonScimSerializer>()
        }
    }

    @Test
    fun `backs off when custom ScimSerializer provided`() {
        val custom = JacksonScimSerializer()
        contextRunner
            .withBean("customSerializer", ScimSerializer::class.java, { custom })
            .run { context ->
                context.getBean(ScimSerializer::class.java) shouldBe custom
            }
    }
}
