# ADR-0004: Java 25 Minimum

## Status

Accepted

## Context

We need to choose a minimum JVM version for the SDK. This affects which language features and APIs are available, as well as the range of applications that can adopt the SDK.

## Decision

We will target Java 25 as the minimum supported version. The `maven-enforcer-plugin` will enforce this at build time.

## Consequences

### Positive

- Access to the latest JVM features (virtual threads, pattern matching, sealed classes in Java, etc.)
- Aligns with modern JVM deployments
- Kotlin compiler can target the latest JVM bytecode for optimal performance

### Negative

- Limits adoption to projects already running on Java 25+
- Excludes organizations on LTS releases (Java 21) that have not yet upgraded
- May require consumers to upgrade their JVM runtime
