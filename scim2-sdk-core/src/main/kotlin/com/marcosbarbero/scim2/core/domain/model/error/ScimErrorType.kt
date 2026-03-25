package com.marcosbarbero.scim2.core.domain.model.error

enum class ScimErrorType(val value: String) {
    INVALID_FILTER("invalidFilter"),
    TOO_MANY("tooMany"),
    UNIQUENESS("uniqueness"),
    MUTABILITY("mutability"),
    INVALID_SYNTAX("invalidSyntax"),
    INVALID_PATH("invalidPath"),
    NO_TARGET("noTarget"),
    INVALID_VALUE("invalidValue"),
    INVALID_VERS("invalidVers"),
    SENSITIVE("sensitive")
}
