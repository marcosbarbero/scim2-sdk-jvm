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
package com.marcosbarbero.scim2.core.schema.annotation

enum class AttributeType {
    STRING, BOOLEAN, DECIMAL, INTEGER, DATE_TIME, BINARY, REFERENCE, COMPLEX
}

enum class Mutability {
    READ_ONLY, READ_WRITE, IMMUTABLE, WRITE_ONLY
}

enum class Returned {
    ALWAYS, NEVER, DEFAULT, REQUEST
}

enum class Uniqueness {
    NONE, SERVER, GLOBAL
}
