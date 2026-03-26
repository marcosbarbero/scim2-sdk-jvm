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
package com.marcosbarbero.scim2.spring.persistence

import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.spring.autoconfigure.ScimJacksonAutoConfiguration
import com.marcosbarbero.scim2.spring.autoconfigure.ScimPersistenceAutoConfiguration
import com.marcosbarbero.scim2.spring.persistence.adapter.JpaResourceRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ScimPersistenceAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfiguration::class.java,
                HibernateJpaAutoConfiguration::class.java,
                JacksonAutoConfiguration::class.java,
                ScimJacksonAutoConfiguration::class.java,
                ScimPersistenceAutoConfiguration::class.java,
            ),
        )
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:scim-autoconfig-test;DB_CLOSE_DELAY=-1",
            "spring.jpa.hibernate.ddl-auto=create-drop",
        )

    @Test
    fun `persistence disabled by default - no JPA repository beans created`() {
        contextRunner
            .run { context ->
                context.containsBean("scimUserRepository") shouldBe false
                context.containsBean("scimGroupRepository") shouldBe false
            }
    }

    @Test
    fun `persistence enabled creates repository beans`() {
        contextRunner
            .withPropertyValues("scim.persistence.enabled=true")
            .run { context ->
                context.getBean("scimUserRepository").shouldNotBeNull()
                context.getBean("scimGroupRepository").shouldNotBeNull()
                context.getBean("scimUserRepository").shouldBeInstanceOf<JpaResourceRepository<*>>()
                context.getBean("scimGroupRepository").shouldBeInstanceOf<JpaResourceRepository<*>>()
            }
    }

    @Test
    fun `backs off when custom user repository provided`() {
        val customRepo = object : ResourceRepository<User> {
            override fun findById(id: String): User? = null
            override fun create(resource: User): User = resource
            override fun replace(id: String, resource: User, version: String?): User = resource
            override fun delete(id: String, version: String?) {}
            override fun search(request: SearchRequest): ListResponse<User> = ListResponse(totalResults = 0, resources = emptyList())
        }

        contextRunner
            .withPropertyValues("scim.persistence.enabled=true")
            .withBean("scimUserRepository", ResourceRepository::class.java, { customRepo })
            .run { context ->
                val repo = context.getBean("scimUserRepository")
                (repo === customRepo) shouldBe true
            }
    }
}
