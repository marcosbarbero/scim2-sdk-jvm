package com.marcosbarbero.scim2.core.filter.lexer

import com.marcosbarbero.scim2.core.domain.model.error.InvalidFilterException

class FilterLexer(private val input: String) {

    private var pos = 0
    private val tokens = mutableListOf<Token>()

    companion object {
        private val KEYWORD_MAP = mapOf(
            "eq" to TokenType.OP_EQ,
            "ne" to TokenType.OP_NE,
            "co" to TokenType.OP_CO,
            "sw" to TokenType.OP_SW,
            "ew" to TokenType.OP_EW,
            "gt" to TokenType.OP_GT,
            "ge" to TokenType.OP_GE,
            "lt" to TokenType.OP_LT,
            "le" to TokenType.OP_LE,
            "pr" to TokenType.OP_PR,
            "and" to TokenType.OP_AND,
            "or" to TokenType.OP_OR,
            "not" to TokenType.OP_NOT
        )
    }

    fun tokenize(): List<Token> {
        while (pos < input.length) {
            skipWhitespace()
            if (pos >= input.length) break

            val ch = input[pos]
            when {
                ch == '"' -> readString()
                ch == '(' -> { tokens.add(Token(TokenType.LPAREN, "(", pos)); pos++ }
                ch == ')' -> { tokens.add(Token(TokenType.RPAREN, ")", pos)); pos++ }
                ch == '[' -> { tokens.add(Token(TokenType.LBRACKET, "[", pos)); pos++ }
                ch == ']' -> { tokens.add(Token(TokenType.RBRACKET, "]", pos)); pos++ }
                ch == '-' || ch.isDigit() -> readNumber()
                ch.isLetter() || ch == '_' -> readWord()
                ch == '.' && pos + 1 < input.length && input[pos + 1].isLetter() -> readWord()
                else -> throw InvalidFilterException(
                    "Unexpected character '$ch' at position $pos"
                )
            }
        }
        tokens.add(Token(TokenType.EOF, "", pos))
        return tokens
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun readString() {
        val start = pos
        pos++ // skip opening quote
        val sb = StringBuilder()
        while (pos < input.length) {
            val ch = input[pos]
            if (ch == '\\' && pos + 1 < input.length) {
                when (input[pos + 1]) {
                    '"' -> { sb.append('"'); pos += 2 }
                    '\\' -> { sb.append('\\'); pos += 2 }
                    'n' -> { sb.append('\n'); pos += 2 }
                    't' -> { sb.append('\t'); pos += 2 }
                    else -> { sb.append(ch); pos++ }
                }
            } else if (ch == '"') {
                pos++ // skip closing quote
                tokens.add(Token(TokenType.STRING_VALUE, sb.toString(), start))
                return
            } else {
                sb.append(ch)
                pos++
            }
        }
        throw InvalidFilterException("Unterminated string starting at position $start")
    }

    private fun readNumber() {
        val start = pos
        if (input[pos] == '-') pos++
        while (pos < input.length && input[pos].isDigit()) pos++
        if (pos < input.length && input[pos] == '.') {
            pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        tokens.add(Token(TokenType.NUMBER_VALUE, input.substring(start, pos), start))
    }

    private fun readWord() {
        val start = pos
        // Read a word that can contain letters, digits, underscores, dots, colons (for URN paths), hyphens
        while (pos < input.length && (input[pos].isLetterOrDigit() || input[pos] == '_' || input[pos] == '.' || input[pos] == ':' || input[pos] == '-')) {
            pos++
        }
        val word = input.substring(start, pos)

        // Check for keywords (case-insensitive), but only if followed by whitespace/end/paren/bracket
        // and not containing dots or colons (which would make it an attribute path)
        val lower = word.lowercase()
        if (!word.contains('.') && !word.contains(':') && KEYWORD_MAP.containsKey(lower)) {
            when (lower) {
                "true" -> tokens.add(Token(TokenType.BOOLEAN_VALUE, "true", start))
                "false" -> tokens.add(Token(TokenType.BOOLEAN_VALUE, "false", start))
                "null" -> tokens.add(Token(TokenType.NULL_VALUE, "null", start))
                else -> tokens.add(Token(KEYWORD_MAP.getValue(lower), word, start))
            }
        } else if (lower == "true" || lower == "false") {
            tokens.add(Token(TokenType.BOOLEAN_VALUE, lower, start))
        } else if (lower == "null") {
            tokens.add(Token(TokenType.NULL_VALUE, "null", start))
        } else {
            tokens.add(Token(TokenType.ATTR_PATH, word, start))
        }
    }
}
