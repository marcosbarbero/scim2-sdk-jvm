package com.marcosbarbero.scim2.test.architecture

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

@AnalyzeClasses(packages = ["com.marcosbarbero.scim2"])
class ArchitectureTest {

    @ArchTest
    val `core module should not depend on server module`: ArchRule = noClasses()
        .that().resideInAPackage("com.marcosbarbero.scim2.core..")
        .should().dependOnClassesThat().resideInAPackage("com.marcosbarbero.scim2.server..")

    @ArchTest
    val `core module should not depend on client module`: ArchRule = noClasses()
        .that().resideInAPackage("com.marcosbarbero.scim2.core..")
        .should().dependOnClassesThat().resideInAPackage("com.marcosbarbero.scim2.client..")

    @ArchTest
    val `core module should not depend on Spring`: ArchRule = noClasses()
        .that().resideInAPackage("com.marcosbarbero.scim2.core..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..")

    @ArchTest
    val `server module should not depend on client module`: ArchRule = noClasses()
        .that().resideInAPackage("com.marcosbarbero.scim2.server..")
        .should().dependOnClassesThat().resideInAPackage("com.marcosbarbero.scim2.client..")

    @ArchTest
    val `server module should not depend on Spring`: ArchRule = noClasses()
        .that().resideInAPackage("com.marcosbarbero.scim2.server..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..")
}
