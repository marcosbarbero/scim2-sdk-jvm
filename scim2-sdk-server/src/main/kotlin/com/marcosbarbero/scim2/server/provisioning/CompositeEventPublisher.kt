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
package com.marcosbarbero.scim2.server.provisioning

import com.marcosbarbero.scim2.core.event.ScimEvent
import com.marcosbarbero.scim2.core.event.ScimEventPublisher
import org.slf4j.LoggerFactory

/**
 * Chains multiple [ScimEventPublisher] instances. Each event is published to
 * all delegates — a failure in one does not prevent others from running.
 *
 * ```java
 * var publisher = new CompositeEventPublisher(List.of(
 *     outboundPublisher,
 *     metricsPublisher,
 *     auditPublisher
 * ));
 * ```
 */
class CompositeEventPublisher(private val delegates: List<ScimEventPublisher>) : ScimEventPublisher {

    private val log = LoggerFactory.getLogger(CompositeEventPublisher::class.java)

    @JvmOverloads
    constructor(vararg delegates: ScimEventPublisher) : this(delegates.toList())

    override fun publish(event: ScimEvent) {
        delegates.forEach { delegate ->
            try {
                delegate.publish(event)
            } catch (e: Exception) {
                log.error("Event publisher {} failed for {}: {}", delegate::class.simpleName, event::class.simpleName, e.message, e)
            }
        }
    }
}
