package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.core.observability.ScimMetrics
import com.marcosbarbero.scim2.spring.observability.MicrometerScimMetrics
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [ScimServerAutoConfiguration::class])
@ConditionalOnClass(MeterRegistry::class)
open class ScimObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry::class)
    @ConditionalOnMissingBean(ScimMetrics::class)
    fun micrometerScimMetrics(registry: MeterRegistry): ScimMetrics =
        MicrometerScimMetrics(registry)
}
