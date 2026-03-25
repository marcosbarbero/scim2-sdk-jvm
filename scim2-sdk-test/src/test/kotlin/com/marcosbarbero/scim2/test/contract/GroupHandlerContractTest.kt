package com.marcosbarbero.scim2.test.contract

import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository
import io.github.serpro69.kfaker.Faker

class GroupHandlerContractTest : ResourceHandlerContractTest<Group>() {

    private val faker = Faker()

    private val repository = InMemoryResourceRepository<Group> { group, id, meta ->
        group.copy(id = id, meta = meta)
    }

    override fun createHandler(): ResourceHandler<Group> {
        repository.clear()
        return InMemoryResourceHandler(
            resourceType = Group::class.java,
            endpoint = "/Groups",
            repository = repository
        )
    }

    override fun sampleResource(): Group = Group(displayName = "Group-${java.util.UUID.randomUUID()}")

    override fun modifiedResource(original: Group): Group =
        original.copy(displayName = "Group-${java.util.UUID.randomUUID()}")
}
