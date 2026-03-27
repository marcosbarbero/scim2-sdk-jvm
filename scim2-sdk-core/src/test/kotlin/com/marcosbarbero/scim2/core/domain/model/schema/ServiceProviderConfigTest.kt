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
package com.marcosbarbero.scim2.core.domain.model.schema

import com.marcosbarbero.scim2.core.domain.ScimUrns
import io.github.serpro69.kfaker.Faker
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class ServiceProviderConfigTest {

    private val faker = Faker()

    @Test
    fun `should include schemas attribute with default ServiceProviderConfig URN`() {
        val config = ServiceProviderConfig()

        config.schemas shouldContainExactly listOf(ScimUrns.SERVICE_PROVIDER_CONFIG)
    }

    @Test
    fun `should create ServiceProviderConfig with defaults`() {
        val config = ServiceProviderConfig()

        config.documentationUri shouldBe null
        config.patch shouldBe ServiceProviderConfig.SupportedConfig()
        config.bulk shouldBe ServiceProviderConfig.BulkConfig()
        config.filter shouldBe ServiceProviderConfig.FilterConfig()
        config.changePassword shouldBe ServiceProviderConfig.SupportedConfig()
        config.sort shouldBe ServiceProviderConfig.SupportedConfig()
        config.etag shouldBe ServiceProviderConfig.SupportedConfig()
        config.authenticationSchemes shouldBe emptyList()
    }

    @Test
    fun `should create ServiceProviderConfig with all fields`() {
        val docUri = faker.internet.domain()
        val authScheme = ServiceProviderConfig.AuthenticationScheme(
            type = "oauthbearertoken",
            name = faker.name.firstName(),
            description = faker.lorem.words(),
            specUri = faker.internet.domain(),
            documentationUri = faker.internet.domain(),
        )

        val config = ServiceProviderConfig(
            documentationUri = docUri,
            patch = ServiceProviderConfig.SupportedConfig(supported = true),
            bulk = ServiceProviderConfig.BulkConfig(supported = true, maxOperations = 1000, maxPayloadSize = 1048576),
            filter = ServiceProviderConfig.FilterConfig(supported = true, maxResults = 200),
            changePassword = ServiceProviderConfig.SupportedConfig(supported = true),
            sort = ServiceProviderConfig.SupportedConfig(supported = true),
            etag = ServiceProviderConfig.SupportedConfig(supported = true),
            authenticationSchemes = listOf(authScheme),
        )

        config.documentationUri shouldBe docUri
        config.patch.supported shouldBe true
        config.bulk.supported shouldBe true
        config.bulk.maxOperations shouldBe 1000
        config.bulk.maxPayloadSize shouldBe 1048576L
        config.filter.supported shouldBe true
        config.filter.maxResults shouldBe 200
        config.changePassword.supported shouldBe true
        config.sort.supported shouldBe true
        config.etag.supported shouldBe true
        config.authenticationSchemes.size shouldBe 1
        config.authenticationSchemes[0].type shouldBe "oauthbearertoken"
        config.authenticationSchemes[0].specUri shouldNotBe null
        config.authenticationSchemes[0].documentationUri shouldNotBe null
    }

    @Test
    fun `SupportedConfig defaults to not supported`() {
        val supported = ServiceProviderConfig.SupportedConfig()

        supported.supported shouldBe false
    }

    @Test
    fun `BulkConfig defaults to not supported with zero limits`() {
        val bulk = ServiceProviderConfig.BulkConfig()

        bulk.supported shouldBe false
        bulk.maxOperations shouldBe 0
        bulk.maxPayloadSize shouldBe 0L
    }

    @Test
    fun `FilterConfig defaults to not supported with zero max results`() {
        val filter = ServiceProviderConfig.FilterConfig()

        filter.supported shouldBe false
        filter.maxResults shouldBe 0
    }

    @Test
    fun `AuthenticationScheme should carry all fields`() {
        val name = faker.name.firstName()
        val desc = faker.lorem.words()
        val scheme = ServiceProviderConfig.AuthenticationScheme(
            type = "httpbasic",
            name = name,
            description = desc,
        )

        scheme.type shouldBe "httpbasic"
        scheme.name shouldBe name
        scheme.description shouldBe desc
        scheme.specUri shouldBe null
        scheme.documentationUri shouldBe null
    }

    @Test
    fun `data class equality for SupportedConfig`() {
        val a = ServiceProviderConfig.SupportedConfig(supported = true)
        val b = ServiceProviderConfig.SupportedConfig(supported = true)
        val c = ServiceProviderConfig.SupportedConfig(supported = false)

        a shouldBe b
        a shouldNotBe c
    }

    @Test
    fun `data class equality for BulkConfig`() {
        val a = ServiceProviderConfig.BulkConfig(supported = true, maxOperations = 10, maxPayloadSize = 100)
        val b = ServiceProviderConfig.BulkConfig(supported = true, maxOperations = 10, maxPayloadSize = 100)
        val c = ServiceProviderConfig.BulkConfig(supported = false, maxOperations = 5, maxPayloadSize = 50)

        a shouldBe b
        a shouldNotBe c
    }

    @Test
    fun `data class equality for FilterConfig`() {
        val a = ServiceProviderConfig.FilterConfig(supported = true, maxResults = 200)
        val b = ServiceProviderConfig.FilterConfig(supported = true, maxResults = 200)
        val c = ServiceProviderConfig.FilterConfig(supported = false, maxResults = 100)

        a shouldBe b
        a shouldNotBe c
    }

    @Test
    fun `data class copy for ServiceProviderConfig`() {
        val original = ServiceProviderConfig(
            documentationUri = "https://example.com/docs",
            patch = ServiceProviderConfig.SupportedConfig(supported = true),
        )

        val copied = original.copy(documentationUri = "https://new.com/docs")

        copied.documentationUri shouldBe "https://new.com/docs"
        copied.patch.supported shouldBe true
    }

    @Test
    fun `data class toString includes class name`() {
        val config = ServiceProviderConfig()
        config.toString().contains("ServiceProviderConfig") shouldBe true
    }

    @Test
    fun `data class hashCode is consistent`() {
        val a = ServiceProviderConfig.AuthenticationScheme(type = "oauth", name = "OAuth", description = "desc")
        val b = ServiceProviderConfig.AuthenticationScheme(type = "oauth", name = "OAuth", description = "desc")

        a.hashCode() shouldBe b.hashCode()
    }
}
