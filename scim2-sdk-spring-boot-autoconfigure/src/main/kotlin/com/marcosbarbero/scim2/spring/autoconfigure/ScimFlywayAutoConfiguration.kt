package com.marcosbarbero.scim2.spring.autoconfigure

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

@AutoConfiguration(after = [ScimPersistenceAutoConfiguration::class])
@ConditionalOnClass(name = ["org.flywaydb.core.Flyway"])
@ConditionalOnProperty(prefix = "scim.persistence", name = ["auto-migrate"], havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ScimProperties::class)
class ScimFlywayAutoConfiguration {

    @Bean
    fun scimFlywayInitializer(
        dataSource: DataSource,
        properties: ScimProperties
    ): FlywayMigrationInitializer {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration/scim")
            .table("scim_flyway_history")
            .baselineOnMigrate(true)
            .apply {
                properties.persistence.schemaName?.let { schemas(it) }
            }
            .load()
        return FlywayMigrationInitializer(flyway)
    }
}
