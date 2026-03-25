# Changelog

All notable changes to the SCIM 2.0 SDK for JVM will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0-M1] - 2026-03-25

### Added
- **Core module** (`scim2-sdk-core`)
  - SCIM 2.0 domain model: User, Group, EnterpriseUserExtension (RFC 7643)
  - Recursive-descent filter parser with visitor pattern (RFC 7644 S3.4.2.2)
  - PATCH operation engine with add/remove/replace and filtered paths (RFC 7644 S3.5.2)
  - Path parser for SCIM attribute paths
  - Schema introspector and registry (annotation-driven)
  - Jackson serialization SPI with ScimModule
  - Event system: ScimEvent sealed hierarchy + ScimEventPublisher SPI
  - Observability SPIs: ScimMetrics, ScimTracer
  - RFC 9457 ProblemDetail support with content negotiation
  - ScimUrns constants for all SCIM URN strings
  - Attribute projector for include/exclude (RFC 7644 S3.4.2.5)
  - ScimValidator for create/replace/patch validation

- **Server module** (`scim2-sdk-server`)
  - ScimEndpointDispatcher with case-insensitive routing
  - All RFC 7644 endpoints: CRUD, search (GET + POST), bulk, /Me, discovery
  - Port interfaces: ResourceHandler, ResourceRepository, IdentityResolver, AuthorizationEvaluator
  - ETag engine with If-Match/If-None-Match (RFC 7644 S3.14)
  - Pagination and bulk engines
  - ScimInterceptor SPI for cross-cutting concerns
  - ScimOutboxPort for transactional event publishing

- **Client module** (`scim2-sdk-client`)
  - Type-safe ScimClient: createUser(), getUser(), searchUsers(), etc.
  - ScimClientBuilder with fluent API
  - Kotlin DSLs: scimFilter {}, scimPatch {}, scimSearch {}
  - AsyncScimClient with Kotlin coroutines
  - Java-friendly ScimClients static methods
  - AuthenticationStrategy SPI (Bearer, Basic)

- **Transport adapters**
  - `scim2-sdk-client-httpclient`: JDK HttpClient adapter
  - `scim2-sdk-client-okhttp`: OkHttp adapter

- **Spring Boot integration** (`scim2-sdk-spring-boot-autoconfigure` + `scim2-sdk-spring-boot-starter`)
  - Auto-configuration with @ConditionalOnMissingBean back-off
  - ScimProperties with IDE autocompletion
  - ScimController (catch-all Spring MVC adapter)
  - ScimExceptionHandler with content negotiation
  - JPA persistence adapter with reference schemas for PostgreSQL, MySQL, Oracle, MSSQL, H2
  - Flyway migration support (opt-in)
  - IdP adapters: Keycloak, Okta, Azure AD, PingFederate, Auth0 (configurable claims)
  - Micrometer metrics adapter

- **Test module** (`scim2-sdk-test`)
  - InMemoryResourceRepository and InMemoryResourceHandler
  - InMemoryScimServer
  - ResourceHandlerContractTest (23 tests per resource type)
  - ScimApiContractTest (19 RFC-referenced HTTP-level tests)
  - ArchUnit architectural boundary tests
  - Pact CDC consumer tests

- **Documentation**
  - OpenAPI 3.1 specification
  - Mermaid architecture diagrams (6 diagrams)
  - Module-level READMEs with Kotlin and Java examples
  - Keycloak integration guide
  - Outbox pattern guide with real-world scenarios
  - HTTP API reference with curl examples
  - ADRs (24 decisions documented)
  - Release guide

- **CI/CD**
  - GitHub Actions: build, test, mutation, release
  - Release via GitHub UI or tag push
  - Codecov integration

[Unreleased]: https://github.com/marcosbarbero/scim2-sdk-jvm/compare/v1.0.0-M1...HEAD
[1.0.0-M1]: https://github.com/marcosbarbero/scim2-sdk-jvm/releases/tag/v1.0.0-M1
