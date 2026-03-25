package com.marcosbarbero.scim2.core.schema.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScimResource(
    val schema: String,
    val name: String,
    val description: String = "",
    val endpoint: String = ""
)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScimAttribute(
    val name: String = "",
    val type: AttributeType = AttributeType.STRING,
    val mutability: Mutability = Mutability.READ_WRITE,
    val returned: Returned = Returned.DEFAULT,
    val uniqueness: Uniqueness = Uniqueness.NONE,
    val required: Boolean = false,
    val caseExact: Boolean = false,
    val multiValued: Boolean = false,
    val description: String = ""
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ScimExtension(
    val schema: String
)
