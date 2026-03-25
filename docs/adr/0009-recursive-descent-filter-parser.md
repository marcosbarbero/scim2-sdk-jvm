# ADR-0009: Recursive Descent Filter Parser

## Status

Accepted

## Context

SCIM defines a filter syntax (RFC 7644, Section 3.4.2.2) that supports attribute comparisons, logical operators (and, or, not), grouping with parentheses, and value path filtering. We need a parser that can handle this grammar and produce an AST for evaluation.

## Decision

We will implement a hand-written recursive descent parser for SCIM filters. The parser will produce a sealed class AST hierarchy that can be evaluated by different backends (in-memory, SQL, LDAP).

## Consequences

### Positive

- Full control over error messages and error recovery
- No external parser generator dependency
- Sealed class AST enables exhaustive `when` matching
- Easy to extend for custom filter operators

### Negative

- More code to write and maintain than a parser generator approach
- Requires thorough testing of edge cases in the grammar
- Contributors must understand recursive descent parsing
