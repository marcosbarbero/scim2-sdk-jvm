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
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
    fun scimHibernateProperties(properties: ScimProperties): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { hibernateProperties ->
            properties.persistence.schemaName?.let {
                hibernateProperties["hibernate.default_schema"] = it
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = ["scimUserRepository"])
    fun scimUserRepository(
        jpaRepository: ScimResourceJpaRepository,
        serializer: ScimSerializer
    ): ResourceRepository<User> = JpaResourceRepository(jpaRepository, serializer, User::class.java, "User")

    @Bean
    @ConditionalOnMissingBean(name = ["scimGroupRepository"])
    fun scimGroupRepository(
        jpaRepository: ScimResourceJpaRepository,
        serializer: ScimSerializer
    ): ResourceRepository<Group> = JpaResourceRepository(jpaRepository, serializer, Group::class.java, "Group")
}
