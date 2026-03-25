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

## Database Schema

### Single-Table JSON Storage Design

The JPA adapter uses a single-table design where all SCIM resource types (Users, Groups, custom types) are stored in one table, discriminated by `resource_type`. The full SCIM resource is preserved as JSON in the `resource_json` column, enabling schema-less flexibility while maintaining queryable metadata columns.

This approach means:
- Any SCIM resource type can be stored without DDL changes
- The full resource fidelity is preserved (no attribute mapping/loss)
- Metadata columns (`resource_type`, `external_id`, `display_name`) enable efficient queries
- ETag-based optimistic concurrency via the `version` column

### Table Structure

```sql
CREATE TABLE scim_resources (
    id              VARCHAR(255) NOT NULL PRIMARY KEY,
    resource_type   VARCHAR(100) NOT NULL,    -- "User", "Group", etc.
    external_id     VARCHAR(255),
    display_name    VARCHAR(500),
    resource_json   TEXT NOT NULL,             -- Full SCIM resource as JSON
    version         BIGINT NOT NULL DEFAULT 1, -- ETag version
    created         TIMESTAMP NOT NULL,
    last_modified   TIMESTAMP NOT NULL
);
```

### Database-Specific SQL Files

- [PostgreSQL](src/main/resources/db/scim/schema-postgresql.sql)
- [MySQL](src/main/resources/db/scim/schema-mysql.sql)
- [Oracle](src/main/resources/db/scim/schema-oracle.sql)
- [MS SQL Server](src/main/resources/db/scim/schema-mssql.sql)
- [H2 (testing)](src/main/resources/db/scim/schema-h2.sql)

### Customizing Table and Schema Name

```yaml
scim:
  persistence:
    enabled: true
    table-name: my_scim_resources   # default: scim_resources
    schema-name: my_schema          # default: datasource default schema
```

### Enabling Auto-Migration

When Flyway is on the classpath, you can opt in to automatic schema creation:

```yaml
scim:
  persistence:
    enabled: true
    auto-migrate: true    # runs Flyway migration for SCIM tables
```

This uses a separate Flyway history table (`scim_flyway_history`) so it does not conflict with your application's own migrations. Requires `org.flywaydb:flyway-core` on the classpath.

## Custom Handler Example

### Kotlin
```kotlin
@Component
class CustomUserHandler(private val userService: UserService) : ResourceHandler<User> {
    override val resourceType = User::class.java
    override val endpoint = "/Users"
    override fun create(resource: User, context: ScimRequestContext) = userService.create(resource)
}
```

### Java
```java
@Component
public class CustomUserHandler implements ResourceHandler<User> {
    private final UserService userService;
    public CustomUserHandler(UserService userService) { this.userService = userService; }
    @Override public Class<User> getResourceType() { return User.class; }
    @Override public String getEndpoint() { return "/Users"; }
    @Override public User create(User resource, ScimRequestContext context) {
        return userService.create(resource);
    }
}
```

## Custom Identity Resolver Example

### Kotlin
```kotlin
@Component
class CustomIdentityResolver : IdentityResolver {
    override fun resolve(request: ScimHttpRequest): ScimRequestContext {
        val token = request.headers["Authorization"]?.removePrefix("Bearer ")
        // your authentication logic
        return ScimRequestContext(subject = token ?: "anonymous")
    }
}
```

### Java
```java
@Component
public class CustomIdentityResolver implements IdentityResolver {
    @Override
    public ScimRequestContext resolve(ScimHttpRequest request) {
        String auth = request.getHeaders().get("Authorization");
        String token = auth != null ? auth.replace("Bearer ", "") : "anonymous";
        return new ScimRequestContext(token);
    }
}
```

## Every bean backs off
Every auto-configured bean uses `@ConditionalOnMissingBean`. Provide your own implementation and the auto-configured one disappears.
