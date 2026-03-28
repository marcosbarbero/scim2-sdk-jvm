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

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.server.config.ScimServerConfig
import com.marcosbarbero.scim2.server.port.ResourceHandler
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.server.port.ScimRequestContext
import com.marcosbarbero.scim2.spring.handler.DefaultResourceHandler
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimServerAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                ScimJacksonAutoConfiguration::class.java,
                ScimServerAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `auto-configures server beans when ResourceHandler is present`() {
        contextRunner
            .withBean(TestUserHandler::class.java)
            .run { context ->
                context.getBean(ScimServerConfig::class.java).shouldNotBeNull()
                context.getBean(SchemaRegistry::class.java).shouldNotBeNull()
                context.getBean(DiscoveryService::class.java).shouldNotBeNull()
                context.getBean(ScimEndpointDispatcher::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `maps properties to ScimServerConfig`() {
        contextRunner
            .withPropertyValues(
                "scim.base-path=/api/scim",
                "scim.base-url=https://example.com",
                "scim.bulk.enabled=false",
                "scim.bulk.max-operations=500",
                "scim.filter.max-results=50",
                "scim.sort.enabled=true",
                "scim.etag.enabled=false",
                "scim.patch.enabled=false",
                "scim.pagination.default-page-size=25",
                "scim.pagination.max-page-size=200",
            )
            .withBean(TestUserHandler::class.java)
            .run { context ->
                val config = context.getBean(ScimServerConfig::class.java)
                config.basePath shouldBe "/api/scim"
                config.baseUrl shouldBe "https://example.com"
                config.bulkEnabled shouldBe false
                config.bulkMaxOperations shouldBe 500
                config.filterMaxResults shouldBe 50
                config.sortEnabled shouldBe true
                config.etagEnabled shouldBe false
                config.patchEnabled shouldBe false
                config.defaultPageSize shouldBe 25
                config.maxPageSize shouldBe 200
            }
    }

    @Test
    fun `backs off when custom ScimServerConfig provided`() {
        val customConfig = ScimServerConfig(basePath = "/custom")
        contextRunner
            .withBean(TestUserHandler::class.java)
            .withBean("customConfig", ScimServerConfig::class.java, { customConfig })
            .run { context ->
                context.getBean(ScimServerConfig::class.java).basePath shouldBe "/custom"
            }
    }

    @Test
    fun `backs off when custom ScimSerializer provided`() {
        contextRunner
            .withBean(TestUserHandler::class.java)
            .withBean("customSerializer", ScimSerializer::class.java, { JacksonScimSerializer() })
            .run { context ->
                context.getBean(ScimSerializer::class.java).shouldBeInstanceOf<JacksonScimSerializer>()
            }
    }

    @Test
    fun `creates dispatcher even without ResourceHandler`() {
        contextRunner
            .run { context ->
                context.getBean(ScimEndpointDispatcher::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `registers ScimModule bean`() {
        contextRunner
            .run { context ->
                context.getBean(com.marcosbarbero.scim2.core.serialization.jackson.ScimModule::class.java).shouldNotBeNull()
            }
    }

    @Test
    fun `DefaultResourceHandler delegates to repository`() {
        val users = mutableMapOf<String, User>()
        val repo = object : ResourceRepository<User> {
            override fun findById(id: String): User? = users[id]
            override fun create(resource: User): User {
                val created = resource.copy(id = "generated-id")
                users["generated-id"] = created
                return created
            }
            override fun replace(id: String, resource: User, version: String?): User {
                users[id] = resource
                return resource
            }
            override fun delete(id: String, version: String?) {
                users.remove(id)
            }
            override fun search(request: SearchRequest): ListResponse<User> = ListResponse(totalResults = users.size, resources = users.values.toList())
        }

        val handler = DefaultResourceHandler(User::class.java, "/Users", repo)
        val context = ScimRequestContext()

        val created = handler.create(User(userName = "testuser"), context)
        created.id shouldBe "generated-id"

        val found = handler.get("generated-id", context)
        found.userName shouldBe "testuser"

        val searchResult = handler.search(SearchRequest(), context)
        searchResult.totalResults shouldBe 1

        handler.delete("generated-id", null, context)
        handler.search(SearchRequest(), context).totalResults shouldBe 0
    }

    @Test
    fun `auto-registers scimUserHandler when UserRepository is present`() {
        val repo = object : ResourceRepository<User> {
            override fun findById(id: String): User? = null
            override fun create(resource: User): User = resource
            override fun replace(id: String, resource: User, version: String?): User = resource
            override fun delete(id: String, version: String?) {}
            override fun search(request: SearchRequest): ListResponse<User> = ListResponse(totalResults = 0, resources = emptyList())
        }

        contextRunner
            .withBean("userRepository", ResourceRepository::class.java, { repo })
            .run { context ->
                context.getBean("scimUserHandler").shouldNotBeNull()
                val handler = context.getBean("scimUserHandler") as ResourceHandler<*>
                handler.endpoint shouldBe "/Users"
                handler.resourceType shouldBe User::class.java
            }
    }

    @Test
    fun `does not create DefaultResourceHandler when no UserRepository`() {
        contextRunner
            .run { context ->
                val handlers = context.getBeansOfType(ResourceHandler::class.java)
                handlers.values.none { it is DefaultResourceHandler<*> } shouldBe true
            }
    }

    @Test
    fun `scimUserHandler backs off when user provides custom handler`() {
        contextRunner
            .withBean("scimUserHandler", ResourceHandler::class.java, { TestUserHandler() })
            .run { context ->
                val handler = context.getBean("scimUserHandler") as ResourceHandler<*>
                handler.shouldBeInstanceOf<TestUserHandler>()
            }
    }

    private class TestUserHandler : ResourceHandler<User> {
        override val resourceType: Class<User> = User::class.java
        override val endpoint: String = "/Users"
        override fun get(id: String, context: ScimRequestContext): User = throw UnsupportedOperationException()
        override fun create(resource: User, context: ScimRequestContext): User = throw UnsupportedOperationException()
        override fun replace(id: String, resource: User, version: String?, context: ScimRequestContext): User = throw UnsupportedOperationException()
        override fun patch(id: String, request: PatchRequest, version: String?, context: ScimRequestContext): User = throw UnsupportedOperationException()
        override fun delete(id: String, version: String?, context: ScimRequestContext) = throw UnsupportedOperationException()
        override fun search(request: SearchRequest, context: ScimRequestContext): ListResponse<User> = throw UnsupportedOperationException()
    }
}
