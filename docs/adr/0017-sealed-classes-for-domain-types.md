# ADR-0017: Sealed Classes for Domain Types

## Status

Accepted

## Context

The SCIM domain model contains several closed type hierarchies: filter AST nodes, patch operations, bulk operation types, and error types. We need a way to model these hierarchies that enables compile-time exhaustiveness checking.

## Decision

We will use Kotlin sealed classes (and sealed interfaces) for all closed type hierarchies in the domain model. Consumers matching on these types with `when` expressions get compile-time guarantees that all cases are handled, without needing a fallback `else` branch.

## Consequences

### Positive

- Compile-time exhaustiveness checking prevents missing-case bugs
- Adding a new subtype causes compilation errors at every `when` site, making it impossible to forget
- Clean pattern matching with smart casts
- Self-documenting: the sealed hierarchy shows all valid variants

### Negative

- Sealed hierarchies cannot be extended by consumers (closed by design)
- All subtypes must be in the same package (Kotlin requirement)
- May require more upfront design to get the hierarchy right
