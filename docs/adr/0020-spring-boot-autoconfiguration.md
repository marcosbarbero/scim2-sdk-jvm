# ADR-0020: Spring Boot Auto-Configuration and Starter

## Status

Accepted

## Context

Applications using this SCIM SDK with Spring Boot need manual wiring of the dispatcher, serializer, discovery service, schema registry, and HTTP adapter. This should be automatic for Spring Boot applications while remaining fully customizable.

## Decision

Introduce two modules following Spring Boot's autoconfigure/starter convention:

### scim2-sdk-spring-boot-autoconfigure

Five auto-configuration classes, ordered by dependency:

1. **ScimJacksonAutoConfiguration** - Registers `ScimModule` into Jackson's `ObjectMapper` and provides a `JacksonScimSerializer` bean. Activates when `ObjectMapper` and `ScimModule` are on the classpath.

2. **ScimServerAutoConfiguration** - Wires `ScimServerConfig` from `ScimProperties` (`@ConfigurationProperties(prefix = "scim")`), creates `SchemaRegistry` (auto-registers resource types from all `ResourceHandler` beans), `DiscoveryService`, and `ScimEndpointDispatcher`. Optional beans (`BulkHandler`, `MeHandler`, `IdentityResolver`, `AuthorizationEvaluator`, `ScimEventPublisher`, `ScimMetrics`, `ScimTracer`, `ScimInterceptor`) are injected via `ObjectProvider` with graceful fallbacks.

3. **ScimClientAutoConfiguration** - Creates `HttpTransport` (defaults to `JavaHttpClientTransport`) and `ScimClient` when `scim.client.base-url` is set. Optional `AuthenticationStrategy` bean is auto-detected.

4. **ScimWebAutoConfiguration** - Registers `ScimController` (Spring MVC adapter mapping `${scim.base-path}/**` to `ScimEndpointDispatcher`) and `ScimExceptionHandler` (`@ControllerAdvice` converting `ScimException` to SCIM JSON error responses). Only activates in servlet web applications.

5. **ScimObservabilityAutoConfiguration** - Provides `MicrometerScimMetrics` (adapting `ScimMetrics` to Micrometer's `MeterRegistry`) when Micrometer is on the classpath.

All auto-configurations use `@ConditionalOnMissingBean` for full back-off support.

### scim2-sdk-spring-boot-starter

Empty JAR aggregating transitive dependencies: autoconfigure, server, client, client-httpclient, and `spring-boot-starter`.

## Consequences

- Spring Boot users add one dependency (`scim2-sdk-spring-boot-starter`) and implement `ResourceHandler` beans
- All beans back off when custom implementations are provided
- Non-Spring applications are unaffected (no framework dependency in core/server)
- Configuration is externalized via `application.properties`/`application.yml` under the `scim.*` prefix
