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

import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import com.marcosbarbero.scim2.spring.event.SpringScimEventPublisher
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimEventAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(ScimEventAutoConfiguration::class.java),
        )

    @Test
    fun `should register SpringScimEventPublisher as default`() {
        contextRunner.run { context ->
            context.containsBean("springScimEventPublisher") shouldBe true
            context.getBean(ScimEventPublisher::class.java).shouldBeInstanceOf<SpringScimEventPublisher>()
        }
    }

    @Test
    fun `should not register SpringScimEventPublisher when custom publisher exists`() {
        contextRunner
            .withBean(ScimEventPublisher::class.java, { com.marcosbarbero.scim2.core.event.NoOpEventPublisher })
            .run { context ->
                context.containsBean("springScimEventPublisher") shouldBe false
            }
    }
}
