# SCIM 2.0 SDK :: Spring Boot Starter

Convenience starter that aggregates all dependencies needed for a Spring Boot SCIM Service Provider.

## Usage

Just add this single dependency:

```xml
<dependency>
    <groupId>com.marcosbarbero</groupId>
    <artifactId>scim2-sdk-spring-boot-starter</artifactId>
    <version>${scim2-sdk.version}</version>
</dependency>
```

This pulls in:
- `scim2-sdk-core` -- domain model and parsing
- `scim2-sdk-server` -- endpoint dispatcher
- `scim2-sdk-client` -- SCIM client
- `scim2-sdk-client-httpclient` -- default HTTP transport
- `scim2-sdk-spring-boot-autoconfigure` -- auto-configuration
- `spring-boot-starter` -- Spring Boot baseline

Configure via `application.yml`:
```yaml
scim:
  base-path: /scim/v2
  persistence:
    enabled: true
```

See [scim2-sdk-spring-boot-autoconfigure](../scim2-sdk-spring-boot-autoconfigure/) for all configuration options.
