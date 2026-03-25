package com.marcosbarbero.scim2.spring.autoconfigure

import com.fasterxml.jackson.databind.ObjectMapper
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.jackson.ScimModule
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [JacksonAutoConfiguration::class])
@ConditionalOnClass(ObjectMapper::class, ScimModule::class)
class ScimJacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun scimModule(): ScimModule = ScimModule()

    @Bean
    @ConditionalOnMissingBean(ScimSerializer::class)
    fun scimSerializer(objectMapper: ObjectMapper): ScimSerializer =
        JacksonScimSerializer(objectMapper)
}
