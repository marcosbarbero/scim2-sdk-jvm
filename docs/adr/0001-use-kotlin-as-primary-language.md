# ADR-0001: Use Kotlin as Primary Language

## Status

Accepted

## Context

We need a modern, expressive JVM language for a SCIM 2.0 SDK that will be consumed by both Kotlin and Java applications. The language must support strong type safety, concise APIs, and seamless interoperability with the Java ecosystem.

## Decision

We will use Kotlin as the primary language with full Java interoperability via `@JvmStatic`, `@JvmOverloads`, `@JvmField`, and related annotations. All public API surfaces will be designed to be idiomatic from both Kotlin and Java call sites.

## Consequences

### Positive

- Enables expressive Kotlin DSLs for client and server configuration
- Built-in null safety reduces NullPointerException risks at the type system level
- Sealed classes and exhaustive `when` expressions model SCIM schema hierarchies cleanly
- Data classes provide immutable value objects with minimal boilerplate
- Full Java interop means consumers can use the SDK from any JVM language

### Negative

- Requires `kotlin-reflect` at runtime for certain features
- Team members must be proficient in Kotlin
- Java consumers need to understand Kotlin conventions (e.g., companion objects, extension functions)
