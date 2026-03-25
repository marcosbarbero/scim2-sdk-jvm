package com.marcosbarbero.scim2.spring.autoconfigure

import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher
import com.marcosbarbero.scim2.spring.web.ScimController
import com.marcosbarbero.scim2.spring.web.ScimExceptionHandler
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [ScimServerAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(HttpServletRequest::class, ScimEndpointDispatcher::class)
@ConditionalOnBean(ScimEndpointDispatcher::class)
open class ScimWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun scimController(dispatcher: ScimEndpointDispatcher): ScimController =
        ScimController(dispatcher)

    @Bean
    @ConditionalOnMissingBean
    fun scimExceptionHandler(serializer: ScimSerializer): ScimExceptionHandler =
        ScimExceptionHandler(serializer)
}
