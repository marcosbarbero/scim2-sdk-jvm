# Contributing

Thank you for your interest in contributing to the SCIM 2.0 SDK for JVM!

## Development Setup

### Prerequisites
- Java 25+
- Maven 3.9+
- Docker (for Testcontainers-based integration tests)

### Build
```bash
mvn clean verify
```

### Run specific module tests
```bash
mvn clean verify -pl scim2-sdk-core -am
```

### Run mutation tests
```bash
mvn verify -Pmutation -pl scim2-sdk-core
```

## Workflow

1. Fork the repository
2. Create a feature branch from `main`
3. Write tests first (TDD)
4. Implement the feature
5. Create an ADR in `docs/adr/` for architectural decisions
6. Open a Pull Request
7. CI must pass (build, tests, coverage)

## Coding Standards

- **Kotlin-first** with full Java interop (`@JvmStatic`, `@JvmOverloads`)
- **No wildcard imports**
- **`internal` by default** -- only explicitly public API is `public`
- **Sealed classes** for closed hierarchies
- **Exhaustive `when`** -- no `else` on sealed classes
- **`require()` / `check()`** over manual if/throw
- **kotlin-faker** for all test data (no hardcoded strings)
- **Extension functions** for type conversions

## Architecture Rules

- `scim2-sdk-core` has ZERO framework dependencies
- Dependencies flow inward: adapters -> ports -> domain
- Every bean in Spring auto-config uses `@ConditionalOnMissingBean`
- All IdP claim names are configurable via properties

## Adding a New IdP Adapter

1. Create a new class extending `JwtIdentityResolver` in `spring/security/`
2. Override `extractRoles()` and `extractAttributes()` for your IdP's claim format
3. Add a `@ConditionalOnProperty` bean in `ScimIdentityAutoConfiguration`
4. Add claim mapping properties to `ClaimMapping`
5. Add tests
6. Update documentation

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
