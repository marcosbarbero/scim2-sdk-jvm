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
package com.marcosbarbero.scim2.spring.event

import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import org.springframework.context.ApplicationEventPublisher

/**
 * Bridges SCIM SDK events to Spring's ApplicationEvent system.
 *
 * This enables:
 * - Spring `@EventListener` methods to react to SCIM resource mutations
 * - Spring Modulith event externalization to forward events to Kafka, AMQP, etc.
 * - namastack-outbox transactional outbox integration via Spring events
 *
 * Auto-configured as the default [ScimEventPublisher] when no custom implementation
 * is provided. Users who need the transactional outbox pattern should provide their
 * own [ScimEventPublisher] bean (e.g., a namastack-outbox adapter).
 *
 * @see <a href="https://github.com/namastack/namastack-outbox">namastack-outbox</a>
 */
class SpringScimEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : ScimEventPublisher {
    override fun publish(event: ScimEvent) {
        applicationEventPublisher.publishEvent(event)
    }
}
