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

import com.marcosbarbero.scim2.core.observability.ScimTracer
import com.marcosbarbero.scim2.spring.observability.OpenTelemetryScimTracer
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration(before = [ScimServerAutoConfiguration::class])
@ConditionalOnClass(name = ["io.opentelemetry.api.trace.Tracer"])
@ConditionalOnProperty(prefix = "scim.tracing", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ScimTracerAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Tracer::class)
    class OpenTelemetryTracerConfiguration {

        @Bean
        @ConditionalOnMissingBean(ScimTracer::class)
        fun openTelemetryScimTracer(tracer: ObjectProvider<Tracer>): ScimTracer? = tracer.ifAvailable?.let { OpenTelemetryScimTracer(it) }
    }
}
