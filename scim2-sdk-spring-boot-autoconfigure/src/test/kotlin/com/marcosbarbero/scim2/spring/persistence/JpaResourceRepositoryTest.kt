package com.marcosbarbero.scim2.spring.persistence

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException
import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.domain.vo.ResourceId
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.spring.persistence.adapter.JpaResourceRepository
import com.marcosbarbero.scim2.spring.persistence.entity.ScimResourceEntity
import com.marcosbarbero.scim2.spring.persistence.repository.ScimResourceJpaRepository
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@EnableJpaRepositories(basePackages = ["com.marcosbarbero.scim2.spring.persistence.repository"])
@EntityScan(basePackages = ["com.marcosbarbero.scim2.spring.persistence.entity"])
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:scim-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    ]
)
class JpaResourceRepositoryTest {

    @Autowired
    private lateinit var jpaRepository: ScimResourceJpaRepository

    private val serializer: ScimSerializer = JacksonScimSerializer()
    private val faker = Faker()

    private lateinit var userRepository: JpaResourceRepository<User>
    private lateinit var groupRepository: JpaResourceRepository<Group>

    @BeforeEach
    fun setUp() {
        jpaRepository.deleteAll()
        userRepository = JpaResourceRepository(jpaRepository, serializer, User::class.java, "User")
        groupRepository = JpaResourceRepository(jpaRepository, serializer, Group::class.java, "Group")
    }

    @Test
    fun `create user persists and returns resource with id and meta`() {
        val userName = faker.name.firstName()
        val user = User(userName = userName, displayName = faker.name.name())

        val created = userRepository.create(user)

        created.id.shouldNotBeNull()
        created.meta.shouldNotBeNull()
        created.meta!!.resourceType shouldBe "User"
        created.meta!!.version.shouldNotBeNull()
        created.userName shouldBe userName
    }

    @Test
    fun `findById returns created user`() {
        val user = User(userName = faker.name.firstName())
        val created = userRepository.create(user)

        val found = userRepository.findById(ResourceId(created.id!!))

        found.shouldNotBeNull()
        found.id shouldBe created.id
        found.userName shouldBe user.userName
    }

    @Test
    fun `findById returns null for non-existent resource`() {
        val result = userRepository.findById(ResourceId("non-existent"))
        result.shouldBeNull()
    }

    @Test
    fun `findById returns null when resource type does not match`() {
        val group = Group(displayName = faker.name.name())
        val created = groupRepository.create(group)

        val result = userRepository.findById(ResourceId(created.id!!))
        result.shouldBeNull()
    }

    @Test
    fun `replace updates the resource and increments version`() {
        val user = User(userName = faker.name.firstName(), displayName = "Original")
        val created = userRepository.create(user)

        val updated = userRepository.replace(
            ResourceId(created.id!!),
            User(userName = created.userName, displayName = "Updated"),
            null
        )

        updated.meta.shouldNotBeNull()
        updated.meta!!.version!!.value shouldBe "W/\"2\""
        updated.meta!!.created shouldBe created.meta!!.created

        val found = userRepository.findById(ResourceId(created.id!!))
        found.shouldNotBeNull()
        found.displayName shouldBe "Updated"
    }

    @Test
    fun `replace throws ResourceNotFoundException for non-existent resource`() {
        assertThrows<ResourceNotFoundException> {
            userRepository.replace(
                ResourceId("non-existent"),
                User(userName = "test"),
                null
            )
        }
    }

    @Test
    fun `delete removes the resource`() {
        val user = User(userName = faker.name.firstName())
        val created = userRepository.create(user)

        userRepository.delete(ResourceId(created.id!!), null)

        userRepository.findById(ResourceId(created.id!!)).shouldBeNull()
    }

    @Test
    fun `delete throws ResourceNotFoundException for non-existent resource`() {
        assertThrows<ResourceNotFoundException> {
            userRepository.delete(ResourceId("non-existent"), null)
        }
    }

    @Test
    fun `search returns paginated results by resource type`() {
        repeat(5) { i ->
            userRepository.create(User(userName = "user-$i"))
        }
        groupRepository.create(Group(displayName = "group-1"))

        val result = userRepository.search(SearchRequest(startIndex = 1, count = 3))

        result.totalResults shouldBe 5
        result.resources.size shouldBe 3
        result.startIndex shouldBe 1
    }

    @Test
    fun `search returns empty list when no resources exist`() {
        val result = userRepository.search(SearchRequest())

        result.totalResults shouldBe 0
        result.resources shouldBe emptyList()
    }

    @Test
    fun `create group persists and returns resource with id and meta`() {
        val displayName = faker.name.name()
        val group = Group(displayName = displayName)

        val created = groupRepository.create(group)

        created.id.shouldNotBeNull()
        created.meta.shouldNotBeNull()
        created.meta!!.resourceType shouldBe "Group"
        created.displayName shouldBe displayName
    }

    @Test
    fun `users and groups are stored independently`() {
        userRepository.create(User(userName = "user-1"))
        groupRepository.create(Group(displayName = "group-1"))

        val userResults = userRepository.search(SearchRequest())
        val groupResults = groupRepository.search(SearchRequest())

        userResults.totalResults shouldBe 1
        groupResults.totalResults shouldBe 1
    }
}
