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

import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.spring.web.ScimController
import com.marcosbarbero.scim2.spring.web.ScimExceptionHandler
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

class ScimWebAutoConfigurationTest {

    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                ScimJacksonAutoConfiguration::class.java,
                ScimServerAutoConfiguration::class.java,
                ScimWebAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `creates ScimController bean when dispatcher is present`() {
        contextRunner
            .run { context ->
                context.getBean(ScimController::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `creates ScimExceptionHandler bean when dispatcher is present`() {
        contextRunner
            .run { context ->
                context.getBean(ScimExceptionHandler::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `backs off ScimController when custom bean provided`() {
        val dispatcher = mockk<ScimEndpointDispatcher>(relaxed = true)
        val customController = ScimController(dispatcher)
        contextRunner
            .withBean("scimController", ScimController::class.java, { customController })
            .run { context ->
                context.getBean(ScimController::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `backs off ScimExceptionHandler when custom bean provided`() {
        val serializer = mockk<ScimSerializer>(relaxed = true)
        val customHandler = ScimExceptionHandler(serializer)
        contextRunner
            .withBean("scimExceptionHandler", ScimExceptionHandler::class.java, { customHandler })
            .run { context ->
                context.getBean(ScimExceptionHandler::class.java).shouldNotBeNull()
            }
    }
}
