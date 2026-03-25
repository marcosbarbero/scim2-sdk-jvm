# ADR-0018: Internal by Default Visibility

## Status

Accepted

## Context

A public SDK must have a well-defined, stable API surface. Exposing too many types as `public` creates a large API surface that is difficult to evolve without breaking changes. Kotlin's `internal` visibility modifier restricts access to the same module, providing a middle ground between `public` and `private`.

## Decision

All classes, functions, and properties will be `internal` by default. Only types that form the explicit public API surface will be marked `public`. This applies to all SDK modules.

## Consequences

### Positive

- Small, deliberate public API surface that is easy to maintain and evolve
- Internal implementation details can change freely between releases
- Reduces risk of consumers depending on unstable internals
- Forces explicit decisions about what is part of the public API

### Negative

- Requires discipline to mark the right things as public
- `internal` types are not accessible from Java (they become `public` with a mangled name)
- May initially frustrate contributors who forget to add `public` visibility
