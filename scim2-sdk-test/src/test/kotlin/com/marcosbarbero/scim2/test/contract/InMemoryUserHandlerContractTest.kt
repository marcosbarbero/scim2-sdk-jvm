package com.marcosbarbero.scim2.test.contract

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import java.util.UUID

class InMemoryUserHandlerContractTest : ResourceHandlerContractTest<User>() {

    override fun createHandler(): ResourceHandler<User> {
        val repository = InMemoryResourceRepository<User> { user, id, meta ->
            user.copy(id = id, meta = meta)
        }
        return InMemoryResourceHandler(
            resourceType = User::class.java,
            endpoint = "/Users",
            repository = repository
        )
    }

    override fun sampleResource(): User = User(
        userName = "testuser-${UUID.randomUUID()}"
    )

    override fun modifiedResource(original: User): User = original.copy(
        displayName = "Modified User ${UUID.randomUUID()}"
    )
}
