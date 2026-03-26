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

import com.marcosbarbero.scim2.core.domain.model.resource.Group
import com.marcosbarbero.scim2.core.domain.model.resource.User
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import com.marcosbarbero.scim2.server.port.ResourceRepository
import com.marcosbarbero.scim2.spring.persistence.adapter.JpaResourceRepository
import com.marcosbarbero.scim2.spring.persistence.repository.ScimResourceJpaRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@AutoConfiguration(after = [ScimJacksonAutoConfiguration::class])
@ConditionalOnClass(name = ["jakarta.persistence.EntityManager"])
@ConditionalOnProperty(prefix = "scim.persistence", name = ["enabled"], havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ScimProperties::class)
@EnableJpaRepositories(basePackages = ["com.marcosbarbero.scim2.spring.persistence.repository"])
@EntityScan(basePackages = ["com.marcosbarbero.scim2.spring.persistence.entity"])
class ScimPersistenceAutoConfiguration {

    @Bean
    fun scimHibernateProperties(properties: ScimProperties): HibernatePropertiesCustomizer = HibernatePropertiesCustomizer { hibernateProperties ->
        properties.persistence.schemaName?.let {
            hibernateProperties["hibernate.default_schema"] = it
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = ["scimUserRepository"])
    fun scimUserRepository(
        jpaRepository: ScimResourceJpaRepository,
        serializer: ScimSerializer,
    ): ResourceRepository<User> = JpaResourceRepository(jpaRepository, serializer, User::class.java, "User")

    @Bean
    @ConditionalOnMissingBean(name = ["scimGroupRepository"])
    fun scimGroupRepository(
        jpaRepository: ScimResourceJpaRepository,
        serializer: ScimSerializer,
    ): ResourceRepository<Group> = JpaResourceRepository(jpaRepository, serializer, Group::class.java, "Group")
}
