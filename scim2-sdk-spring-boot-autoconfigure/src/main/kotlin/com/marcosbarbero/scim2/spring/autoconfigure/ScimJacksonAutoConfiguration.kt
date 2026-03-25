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
import com.marcosbarbero.scim2.core.serialization.jackson.ScimModule
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import tools.jackson.databind.ObjectMapper

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
