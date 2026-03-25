# ADR-0011: Generic Core with OOTB IdP Adapters

## Status
Accepted (updated 2026-03-25, originally accepted as "No IdP Opinion")

## Context
SCIM is inherently about identity provisioning — most deployments integrate with a specific Identity Provider. Requiring every user to implement `IdentityResolver` and `AuthenticationStrategy` from scratch creates unnecessary boilerplate for common IdPs. The SDK should work out of the box with major providers while remaining extensible for custom or niche providers.

## Decision
- Core and server modules define generic interfaces with zero IdP dependencies:
  - `IdentityResolver` — resolves the authenticated principal from request context
  - `AuthenticationStrategy` — handles token validation and authentication
  - `AuthorizationEvaluator` — evaluates fine-grained authorization decisions
- The Spring Boot autoconfigure module provides OOTB adapters for major IdPs:
  - **Okta** — maps Okta JWT claims to SCIM identity
  - **Azure AD (Entra ID)** — maps Azure AD / Microsoft Entra tokens and claims
  - **Keycloak** — maps Keycloak realm tokens and roles
  - **PingFederate** — maps PingFederate access tokens
  - **Auth0** — maps Auth0 JWT claims
- Each IdP adapter:
  - Auto-detected via `@ConditionalOnClass` for the respective IdP SDK on classpath
  - Configurable via `scim.idp.*` properties (issuer URL, client ID, claim mappings, etc.)
  - Backs off via `@ConditionalOnMissingBean` so users can provide their own implementation
- Additional IdP adapters can be contributed as separate modules or added to autoconfigure

## Consequences
- Works OOTB with the 5 most popular IdPs — no boilerplate needed
- Fully extensible for custom, on-premise, or niche identity providers
- Core/server modules remain clean of IdP SDK dependencies
- Each new IdP adapter is an additive change
- Aligns with Spring Security's OIDC/SAML resource server integration path
