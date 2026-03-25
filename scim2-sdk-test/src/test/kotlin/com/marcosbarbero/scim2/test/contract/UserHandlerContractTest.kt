package com.marcosbarbero.scim2.test.contract

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import io.github.serpro69.kfaker.Faker

class UserHandlerContractTest : ResourceHandlerContractTest<User>() {

    private val faker = Faker()

    private val repository = InMemoryResourceRepository<User> { user, id, meta ->
        user.copy(id = id, meta = meta)
    }

    override fun createHandler(): ResourceHandler<User> {
        repository.clear()
        return InMemoryResourceHandler(
            resourceType = User::class.java,
            endpoint = "/Users",
            repository = repository
        )
    }

    override fun sampleResource(): User = User(userName = faker.name.firstName())

    override fun modifiedResource(original: User): User =
        original.copy(displayName = faker.name.name())
}
