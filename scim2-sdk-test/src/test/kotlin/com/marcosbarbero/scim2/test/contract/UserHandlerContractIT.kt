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
package com.marcosbarbero.scim2.test.contract

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import io.github.serpro69.kfaker.Faker

class UserHandlerContractIT : ResourceHandlerContractTest<User>() {

    private val faker = Faker()

    private val repository = InMemoryResourceRepository<User> { user, id, meta ->
        user.copy(id = id, meta = meta)
    }

    override fun createHandler(): ResourceHandler<User> {
        repository.clear()
        return InMemoryResourceHandler(
            resourceType = User::class.java,
            endpoint = "/Users",
            repository = repository,
        )
    }

    override fun sampleResource(): User = User(userName = faker.name.firstName())

    override fun modifiedResource(original: User): User = original.copy(displayName = faker.name.name())
}
