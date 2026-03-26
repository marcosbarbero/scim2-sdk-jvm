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

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimFlywayAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                ScimFlywayAutoConfiguration::class.java,
            ),
        )
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:scim_flyway_test;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
        )

    @Test
    fun `does not activate when auto-migrate is false`() {
        contextRunner
            .withPropertyValues("scim.persistence.auto-migrate=false")
            .run { context ->
                context.containsBean("scimFlywayInitializer") shouldBe false
            }
    }

    @Test
    fun `does not activate when auto-migrate property is missing`() {
        contextRunner
            .run { context ->
                context.containsBean("scimFlywayInitializer") shouldBe false
            }
    }

    @Test
    fun `activates when auto-migrate is true and Flyway on classpath`() {
        contextRunner
            .withPropertyValues("scim.persistence.auto-migrate=true")
            .run { context ->
                context.containsBean("scimFlywayInitializer") shouldBe true
            }
    }
}
