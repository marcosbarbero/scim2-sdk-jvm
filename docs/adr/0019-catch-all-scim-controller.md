# ADR-0019: Catch-All SCIM Controller

## Status
Accepted

## Context
The Spring Boot auto-configuration needs to expose SCIM endpoints. Two approaches were considered:
1. **Explicit controllers**: Separate `@RestController` per endpoint type (ScimUserController, ScimGroupController, ScimDiscoveryController, ScimBulkController)
2. **Catch-all controller**: Single `ScimController` with `@RequestMapping("/**")` under the SCIM base path, delegating all routing to `ScimEndpointDispatcher`

## Decision
Use the catch-all controller approach.

The `ScimController` is a thin Spring MVC adapter that bridges `HttpServletRequest` to `ScimHttpRequest`, delegates to `ScimEndpointDispatcher`, and maps `ScimHttpResponse` back to `ResponseEntity`. All routing logic lives in the framework-agnostic dispatcher.

## Consequences
**Benefits:**
- The routing logic is in `ScimEndpointDispatcher` (framework-agnostic), not in Spring annotations — keeps the server module portable
- Custom resource types (e.g., `/Employees`, `/Devices`) are automatically handled without new controllers
- Case-insensitive routing is handled once in the dispatcher, not per-controller
- Single point of entry for interceptors, metrics, tracing, and event publishing

**Trade-offs:**
- Less visibility in Spring Boot Actuator's endpoint mapping (shows as single `/**` pattern)
- Swagger/OpenAPI auto-generation doesn't see individual endpoints (manual OpenAPI spec recommended)
- Spring Security `requestMatchers` must use path patterns rather than controller-specific rules
