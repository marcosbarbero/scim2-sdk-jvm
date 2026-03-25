# ADR-0005: Pluggable HTTP Transport SPI

## Status

Accepted

## Context

The SCIM client module needs to make HTTP requests but should not force a specific HTTP library on consumers. Different projects already have established HTTP stacks (Java HttpClient, OkHttp, Apache HttpClient) and introducing a conflicting dependency creates classpath issues.

## Decision

We will define an `HttpTransport` interface in the client module as a Service Provider Interface (SPI). Concrete adapters will be provided in separate modules:

- `scim2-sdk-client-httpclient` — Java `HttpClient` adapter (JDK built-in)
- `scim2-sdk-client-okhttp` — OkHttp adapter

Consumers choose their adapter by adding the corresponding dependency. The Spring Boot starter defaults to the Java HttpClient adapter.

## Consequences

### Positive

- No forced HTTP library — consumers use what they already have
- No classpath conflicts with existing dependencies
- Easy to add new adapters (e.g., Apache HttpClient, Ktor) without changing core client code
- Transport can be mocked easily in tests

### Negative

- More modules and code to maintain
- Consumers must explicitly choose and add a transport adapter dependency
- SPI abstraction adds a thin layer of indirection
