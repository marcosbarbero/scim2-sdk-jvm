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
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean

/**
 * Auto-configures [SpringScimEventPublisher] as the default [ScimEventPublisher].
 *
 * This bridges SCIM events to Spring's ApplicationEvent system, enabling
 * @EventListener, Spring Modulith, and namastack-outbox integrations.
 *
 * Users who provide their own [ScimEventPublisher] bean (e.g., a custom outbox
 * adapter) will take precedence over this default.
 */
@AutoConfiguration(before = [ScimServerAutoConfiguration::class])
class ScimEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ScimEventPublisher::class)
    fun springScimEventPublisher(applicationEventPublisher: ApplicationEventPublisher): ScimEventPublisher = SpringScimEventPublisher(applicationEventPublisher)
}
