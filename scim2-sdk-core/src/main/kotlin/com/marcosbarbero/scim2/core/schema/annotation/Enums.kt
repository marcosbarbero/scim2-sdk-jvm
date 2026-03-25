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
