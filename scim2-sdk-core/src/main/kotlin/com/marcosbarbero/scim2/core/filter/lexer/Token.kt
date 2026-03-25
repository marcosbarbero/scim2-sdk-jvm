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
package com.marcosbarbero.scim2.core.filter.lexer

enum class TokenType {
    ATTR_PATH,
    STRING_VALUE, NUMBER_VALUE, BOOLEAN_VALUE, NULL_VALUE,
    OP_EQ, OP_NE, OP_CO, OP_SW, OP_EW, OP_GT, OP_GE, OP_LT, OP_LE, OP_PR,
    OP_AND, OP_OR, OP_NOT,
    LPAREN, RPAREN, LBRACKET, RBRACKET,
    EOF
}

data class Token(val type: TokenType, val value: String, val position: Int)
