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

import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer
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
        properties: ScimProperties,
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
