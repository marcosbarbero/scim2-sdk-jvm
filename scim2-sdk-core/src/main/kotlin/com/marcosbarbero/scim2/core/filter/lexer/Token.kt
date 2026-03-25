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
