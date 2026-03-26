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
package com.marcosbarbero.scim2.client.dsl

import com.marcosbarbero.scim2.core.domain.ScimUrns
import com.marcosbarbero.scim2.core.domain.model.search.SortOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SearchDslTest {

    @Test
    fun `scimSearch with filter DSL sortBy and count produces correct SearchRequest`() {
        val request = scimSearch {
            filter { userName sw "j" }
            sortBy("userName")
            count(25)
        }
        request.schemas shouldBe listOf(ScimUrns.SEARCH_REQUEST)
        request.filter shouldBe """userName sw "j""""
        request.sortBy shouldBe "userName"
        request.count shouldBe 25
        request.sortOrder shouldBe SortOrder.ASCENDING
        request.startIndex shouldBe 1
    }

    @Test
    fun `scimSearch with string filter`() {
        val request = scimSearch { filter("""userName eq "john"""") }
        request.filter shouldBe """userName eq "john""""
    }

    @Test
    fun `scimSearch with sortOrder descending`() {
        val request = scimSearch {
            sortBy("displayName")
            sortOrder(SortOrder.DESCENDING)
        }
        request.sortOrder shouldBe SortOrder.DESCENDING
    }

    @Test
    fun `scimSearch with attributes and excludedAttributes`() {
        val request = scimSearch {
            attributes("userName", "displayName")
            excludedAttributes("emails")
        }
        request.attributes shouldBe listOf("userName", "displayName")
        request.excludedAttributes shouldBe listOf("emails")
    }

    @Test
    fun `scimSearch with startIndex`() {
        val request = scimSearch {
            startIndex(10)
            count(50)
        }
        request.startIndex shouldBe 10
        request.count shouldBe 50
    }

    @Test
    fun `scimSearch with defaults produces minimal request`() {
        val request = scimSearch { }
        request.filter shouldBe null
        request.sortBy shouldBe null
        request.sortOrder shouldBe SortOrder.ASCENDING
        request.startIndex shouldBe 1
        request.count shouldBe null
        request.attributes shouldBe null
        request.excludedAttributes shouldBe null
    }

    @Test
    fun `scimSearch with complex filter DSL`() {
        val request = scimSearch {
            filter { (userName sw "j") and (active eq true) }
            count(10)
        }
        request.filter shouldBe """userName sw "j" and active eq true"""
    }
}
