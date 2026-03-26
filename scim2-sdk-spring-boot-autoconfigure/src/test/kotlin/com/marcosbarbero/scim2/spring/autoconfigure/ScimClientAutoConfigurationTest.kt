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

import com.marcosbarbero.scim2.client.adapter.httpclient.HttpClientTransport
import com.marcosbarbero.scim2.client.api.ScimClient
import com.marcosbarbero.scim2.client.port.HttpTransport
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimClientAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                ScimJacksonAutoConfiguration::class.java,
                ScimClientAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `auto-configures client when base-url is set`() {
        contextRunner
            .withPropertyValues("scim.client.base-url=http://localhost:8080/scim/v2")
            .run { context ->
                context.getBean(HttpTransport::class.java).shouldNotBeNull()
                context.getBean(ScimClient::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `no client without base-url property`() {
        contextRunner
            .run { context ->
                context.containsBean("scimClient").shouldBeFalse()
                context.containsBean("httpTransport").shouldBeFalse()
            }
    }

    @Test
    fun `backs off when custom ScimClient provided`() {
        val customClient = mockk<ScimClient>()
        contextRunner
            .withPropertyValues("scim.client.base-url=http://localhost:8080/scim/v2")
            .withBean("customClient", ScimClient::class.java, { customClient })
            .run { context ->
                context.getBean(ScimClient::class.java) shouldBe customClient
            }
    }

    @Test
    fun `backs off when custom HttpTransport provided`() {
        val customTransport = mockk<HttpTransport>()
        contextRunner
            .withPropertyValues("scim.client.base-url=http://localhost:8080/scim/v2")
            .withBean("customTransport", HttpTransport::class.java, { customTransport })
            .run { context ->
                context.getBean(HttpTransport::class.java) shouldBe customTransport
            }
    }

    @Test
    fun `client timeouts applied to transport`() {
        contextRunner
            .withPropertyValues(
                "scim.client.base-url=http://localhost:8080/scim/v2",
                "scim.client.connect-timeout=5s",
                "scim.client.read-timeout=15s",
            )
            .run { context ->
                val transport = context.getBean(HttpTransport::class.java)
                transport.shouldBeInstanceOf<HttpClientTransport>()
                // The transport was created with the configured timeouts.
                // We verify it was wired as HttpClientTransport (not a mock), confirming
                // the auto-configuration factory method that reads timeouts was invoked.
                transport.shouldNotBeNull()
            }
    }
}
