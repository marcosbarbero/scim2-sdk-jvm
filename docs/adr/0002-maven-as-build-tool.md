# ADR-0002: Maven as Build Tool

## Status

Accepted

## Context

We need a reliable build tool that supports Kotlin compilation, multi-module projects, and publishing to Maven Central. The two main candidates are Maven and Gradle.

## Decision

We will use Maven with the `kotlin-maven-plugin` for compilation and `flatten-maven-plugin` for CI-friendly `${revision}` versioning. The build uses a reactor POM with all modules declared in a single parent.

## Consequences

### Positive

- Well-documented, battle-tested publishing pipeline to Maven Central via `central-publishing-maven-plugin`
- Industry standard for Java/Kotlin library publishing
- Deterministic builds with explicit plugin configuration
- Wide IDE support (IntelliJ, Eclipse, VS Code)

### Negative

- Less flexible than Gradle for custom build logic
- XML verbosity compared to Gradle Kotlin DSL
- Mixed Kotlin/Java compilation requires careful plugin ordering (kotlin-maven-plugin must execute before maven-compiler-plugin)
