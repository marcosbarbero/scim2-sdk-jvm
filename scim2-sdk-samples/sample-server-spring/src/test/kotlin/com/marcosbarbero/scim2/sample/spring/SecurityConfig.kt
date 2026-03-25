package com.marcosbarbero.scim2.sample.spring

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.resourceserver.jwt",
    name = ["issuer-uri"]
)
open class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .authorizeHttpRequests { it.anyRequest().authenticated() }
        .oauth2ResourceServer { it.jwt {} }
        .csrf { it.disable() }
        .build()
}
