package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
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
