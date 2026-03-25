package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.client.api.ScimClient
import com.marcosbarbero.scim2.client.port.HttpTransport
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimClientAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                ScimJacksonAutoConfiguration::class.java,
                ScimClientAutoConfiguration::class.java
            )
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
}
