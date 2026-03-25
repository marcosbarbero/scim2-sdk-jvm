# ADR-0022: OOTB IdP Adapters via Spring Security JWT

## Status
Accepted

## Context
The SDK's `IdentityResolver` interface requires every user to implement JWT claim extraction logic, even for well-known identity providers like Keycloak, Okta, Azure AD, PingFederate, and Auth0. Each IdP uses different JWT claim structures for roles, principals, and user attributes. This creates repetitive boilerplate for common deployments.

## Decision
We provide out-of-the-box `IdentityResolver` implementations for five major IdPs in the Spring Boot autoconfigure module:

- **`JwtIdentityResolver`** -- Base class that extracts identity from Spring Security's `JwtAuthenticationToken` using configurable claim names (`sub`, `roles`, `email`).
- **`KeycloakIdentityResolver`** -- Extracts realm roles from `realm_access.roles` and optional client roles from `resource_access.{clientId}.roles`.
- **`OktaIdentityResolver`** -- Extracts roles from `groups` and `scp` (scopes) claims.
- **`AzureAdIdentityResolver`** -- Uses `oid` as principal, extracts `roles` and `wids` (directory role IDs), plus `tid` (tenant ID).
- **`PingFederateIdentityResolver`** -- Extracts roles from `memberOf` and `groups` claims.
- **`Auth0IdentityResolver`** -- Extracts roles from `{namespace}/roles` and `permissions` claims.

Selection is driven by the `scim.idp.provider` property. Each bean uses `@ConditionalOnMissingBean(IdentityResolver::class)` so a user-provided implementation always takes precedence. The entire auto-configuration activates only when `org.springframework.security.oauth2.jwt.Jwt` is on the classpath (`@ConditionalOnClass`).

Spring Security OAuth2 Resource Server and JOSE dependencies are declared as `<optional>true</optional>` in the autoconfigure module, so they are not transitively pulled in.

## Consequences
- Zero-config identity resolution for the five most common IdPs.
- The `JwtIdentityResolver` base class is `open`, so users can extend it for IdPs not covered OOTB.
- Core and server modules remain free of Spring Security or IdP dependencies.
- Adding a new IdP adapter is an additive, non-breaking change.
- The `@ConditionalOnProperty` + `@ConditionalOnMissingBean` pattern ensures deterministic bean selection and clean back-off.
