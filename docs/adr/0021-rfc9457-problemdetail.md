# ADR-0021: RFC 9457 ProblemDetail Support

## Status
Accepted

## Context
SCIM 2.0 (RFC 7644) defines its own error format (`urn:ietf:params:scim:api:messages:2.0:Error`). However, RFC 9457 (Problem Details for HTTP APIs) has become the industry standard for HTTP API error responses. Many API consumers and tooling (including Spring Boot 3.x) expect ProblemDetail format.

## Decision
Support both error formats via content negotiation:
- `Accept: application/problem+json` → respond with RFC 9457 ProblemDetail
- `Accept: application/scim+json` (or default) → respond with SCIM Error format

The `ScimProblemDetail` type maps SCIM error fields to ProblemDetail:
- `scimType` → `type` URI (prefixed with `urn:ietf:params:scim:api:messages:2.0:Error:`)
- `status` → `status`
- `detail` → `detail`
- SCIM's `scimType` also preserved as an extension field for backwards compatibility

## Consequences
- Clients can choose their preferred error format via Accept header
- Spring Boot's native ProblemDetail support aligns naturally
- SCIM-specific error information is never lost
- Slight complexity in error handling (two formats), but contained in one place
