# SCIM 2.0 SDK for JVM

A modern, Kotlin-first SCIM 2.0 (RFC 7643/7644) SDK for the JVM with full Java interop.

## Modules

| Module | Description |
|---|---|
| `scim2-sdk-core` | Domain model, filter/path parsing, PATCH engine, serialization SPI |
| `scim2-sdk-server` | Server framework with hexagonal architecture (ports + adapters) |
| `scim2-sdk-client` | Fluent client API with Kotlin DSLs |
| `scim2-sdk-client-httpclient` | Java HttpClient transport adapter |
| `scim2-sdk-client-okhttp` | OkHttp transport adapter |
| `scim2-sdk-spring-boot-autoconfigure` | Spring Boot auto-configuration |
| `scim2-sdk-spring-boot-starter` | Spring Boot starter |
| `scim2-sdk-test` | Test fixtures, contract tests, in-memory server |

## Requirements

- Java 25+
- Maven 3.9+

## License

[Apache License 2.0](LICENSE)
