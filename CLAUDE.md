# CLAUDE.md - SCIM 2.0 SDK for JVM

## Project Overview
A SCIM 2.0 (RFC 7643/7644) SDK for the JVM, written in Kotlin with full Java interop.
Three-tier: core (domain model, parsing, SPI), server (handler framework), client (fluent API + DSLs).
Designed for potential integration with Spring Modulith as an enhancement to Spring Security OIDC/SAML.

## Build & Test Commands
- Full build: `mvn clean verify`
- Single module: `mvn clean verify -pl scim2-sdk-core`
- Mutation tests: `mvn verify -Pmutation -pl scim2-sdk-core`
- Integration tests (Testcontainers): `mvn verify -Pintegration`
- Coverage report: `mvn jacoco:report` (target/site/jacoco)
- Docs: `mvn dokka:dokka`

## Workflow — ALL changes must go through PRs
1. Create a feature branch from `main`
2. Implement using TDD (tests first)
3. Create ADR in `docs/adr/` for any architectural decision
4. Open a PR — CI must pass (build, tests, coverage, mutation)
5. PR is reviewed and merged

## Team Roles (for each feature/phase)
| Role | Responsibility |
|---|---|
| Tech Lead | ADR, interface design, acceptance criteria, final approval |
| Kotlin Expert | Core implementation, DSLs, idiomatic Kotlin, Java interop |
| Spring Security Expert | Spring auto-config, security, OIDC/SAML alignment |
| Code Reviewer | Quality review, SOLID, test coverage, edge cases |

## Architecture Rules
1. scim2-sdk-core has ZERO framework dependencies (kotlin-stdlib, kotlin-reflect, jackson-optional)
2. Hexagonal: domain/ → ports/ → adapters/. Dependencies flow inward only.
3. All classes `internal` by default. Only explicit public API is `public`.
4. Value objects are immutable (data class / @JvmInline value class).
5. No `Any` or `Object` in public API.
6. **No database opinions.** All persistence is behind generic interfaces (`ResourceRepository<T>`). No JPA, no JDBC, no specific DB.
7. **No IdP opinions.** Authentication/authorization is behind generic interfaces (`IdentityResolver`, `AuthenticationStrategy`). No specific IdP integration in core/server.
8. Every architectural decision requires an ADR in `docs/adr/`.

## Coding Conventions
- Kotlin primary, Java interop via @JvmStatic, @JvmOverloads, @JvmField
- No wildcard imports
- sealed class for closed hierarchies (AST nodes, exceptions, events)
- Exhaustive `when` (no `else` on sealed classes)
- Prefer require() / check() over manual if/throw
- Test naming: `{ProductionClass}Test`, backtick-quoted descriptive methods

## TDD Workflow
1. Write failing test → 2. Minimum code to pass → 3. Refactor green → 4. Check mutation score

## Testing
- Unit: JUnit 5 + Kotest assertions + MockK
- Integration: Testcontainers (for any tests needing infrastructure)
- Mutation: PITest with 75% threshold
- Contract: `ResourceHandlerContractTest` base class

## Key RFCs
- RFC 7643: Core Schema — https://www.rfc-editor.org/rfc/rfc7643.html
- RFC 7644: Protocol — https://www.rfc-editor.org/rfc/rfc7644.html
