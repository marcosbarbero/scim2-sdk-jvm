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
package com.marcosbarbero.scim2.core.event

import org.junit.jupiter.api.Test

class ScimEventPublisherTest {

    @Test
    fun `NoOpEventPublisher should not throw on publish`() {
        val event = ResourceCreatedEvent(resourceType = "User", resourceId = "1")
        NoOpEventPublisher.publish(event)
    }
}
