# SCIM 2.0 SDK :: Spring Boot Auto-Configuration

Auto-configures the SCIM 2.0 SDK for Spring Boot applications. Provides sensible defaults with full customization via properties and `@ConditionalOnMissingBean` back-off.

## What gets auto-configured

| Feature | Condition | Property |
|---|---|---|
| SCIM endpoints (`/scim/v2/*`) | `ScimEndpointDispatcher` on classpath | `scim.base-path` |
| JPA persistence | JPA on classpath | `scim.persistence.enabled=true` |
| Flyway migration | Flyway on classpath | `scim.persistence.auto-migrate=true` |
| SCIM client | -- | `scim.client.base-url` |
| IdP adapter | Spring Security OAuth2 on classpath | `scim.idp.provider` |
| Micrometer metrics | Micrometer on classpath | automatic |
| Jackson serialization | Jackson on classpath | automatic |

## IdP Adapters

Configure your Identity Provider for JWT-based authentication:

```yaml
scim:
  idp:
    provider: keycloak  # keycloak, okta, azure-ad, ping-federate, auth0
    client-id: my-app   # for Keycloak client roles
    claims:              # override default claim names
      subject: sub
      roles: realm_access
      email: email
```

Supported providers: Keycloak, Okta, Azure AD (Entra ID), PingFederate, Auth0.

All claim names are configurable via `scim.idp.claims.*`. Provide your own `IdentityResolver` bean to replace any adapter.

## Persistence

```yaml
scim:
  persistence:
    enabled: true           # enable JPA-backed storage
    table-name: scim_resources  # customize table name
    schema-name: my_schema  # optional DB schema
    auto-migrate: true      # run Flyway migration on startup
```

Reference SQL schemas for PostgreSQL, MySQL, Oracle, MSSQL, H2 are in `src/main/resources/db/scim/`.

## Every bean backs off
Every auto-configured bean uses `@ConditionalOnMissingBean`. Provide your own implementation and the auto-configured one disappears.
