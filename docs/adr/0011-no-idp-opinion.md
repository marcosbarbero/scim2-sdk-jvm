# ADR-0011: No IdP Opinion

## Status

Accepted

## Context

SCIM is often deployed alongside identity providers (IdPs) with OIDC, SAML, or other authentication mechanisms. Different organizations use different IdPs (Okta, Azure AD, Keycloak, custom) and authentication strategies.

## Decision

Authentication and authorization in the SDK are behind generic interfaces (`IdentityResolver`, `AuthenticationStrategy`). The SDK will not include any IdP-specific dependencies in core or server modules.

## Consequences

### Positive

- SDK works with any IdP or authentication mechanism
- No transitive IdP SDK dependencies
- Implementers choose their own security stack
- Spring Boot starter can optionally integrate with Spring Security

### Negative

- Implementers must wire their own authentication
- Cannot provide turnkey IdP integration without additional modules
- Security misconfiguration risk if implementers skip authentication
