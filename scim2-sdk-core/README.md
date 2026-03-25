# SCIM 2.0 SDK :: Core

The foundation module containing the SCIM 2.0 domain model, protocol types, filter/path parsing engine, PATCH operation engine, and serialization SPI. This module has **zero framework dependencies** -- only Kotlin stdlib, Kotlin reflect, and Jackson (optional).

## What's in this module

### Domain Model (RFC 7643)
Pre-built Kotlin data classes for all SCIM 2.0 resource types:
- `User` -- all RFC 7643 attributes (userName, name, emails, phoneNumbers, addresses, etc.)
- `Group` -- displayName, members
- `EnterpriseUserExtension` -- employeeNumber, department, manager, etc.
- `ScimResource` -- abstract base class for all resources

### Protocol Types (RFC 7644)
- `PatchRequest` / `PatchOperation` -- SCIM PATCH operations
- `SearchRequest` / `ListResponse` -- search queries and paginated results
- `BulkRequest` / `BulkResponse` -- bulk operations
- `ScimError` / `ScimProblemDetail` -- error responses (RFC 7644 + RFC 9457)

### Filter Parser
A complete recursive-descent parser for SCIM filter expressions:

#### Kotlin
```kotlin
val filter = FilterParser.parse("userName eq \"john\" and active eq true")
// Produces an AST: LogicalExpression(AND, AttributeExpression(...), AttributeExpression(...))
```

#### Java
```java
var filter = FilterParser.parse("userName eq \"john\" and active eq true");
// Produces an AST: LogicalExpression(AND, AttributeExpression(...), AttributeExpression(...))
```

Includes `FilterVisitor<T>` for transforming filters (to SQL, to predicates, to string, etc.)

### Path Parser
Parses SCIM PATCH path expressions:

#### Kotlin
```kotlin
val path = PathParser.parse("emails[type eq \"work\"].value")
// FilteredPath(attributeName="emails", filter=..., subAttribute="value")
```

#### Java
```java
var path = PathParser.parse("emails[type eq \"work\"].value");
// FilteredPath(attributeName="emails", filter=..., subAttribute="value")
```

### PATCH Engine
Applies SCIM PATCH operations to resources immutably:

#### Kotlin
```kotlin
val engine = PatchEngine(objectMapper)
val updated = engine.apply(user, patchRequest)
```

#### Java
```java
var engine = new PatchEngine(objectMapper);
var updated = engine.apply(user, patchRequest);
```

### Extension Schemas

SCIM supports extension schemas (RFC 7643 S3.3). The SDK includes the Enterprise User extension:

#### Kotlin
```kotlin
val user = User(userName = "jane.doe")
user.setExtension(
    ScimUrns.ENTERPRISE_USER,
    EnterpriseUserExtension(
        employeeNumber = "12345",
        department = "Engineering",
        manager = Manager(value = "manager-id", displayName = "John Boss")
    )
)
```

#### Java
```java
var user = new User("jane.doe");
user.setExtension(
    ScimUrns.ENTERPRISE_USER,
    new EnterpriseUserExtension("12345", "Engineering",
        new Manager("manager-id", "John Boss"))
);
```

Custom extensions can be defined using `@ScimExtension`:

```kotlin
@ScimExtension(schema = "urn:example:custom:1.0:Department")
data class DepartmentExtension(
    val departmentCode: String,
    val costCenter: String
)
```

### Serialization SPI
`ScimSerializer` interface with `JacksonScimSerializer` as the default implementation.

### Event & Observability SPIs
- `ScimEvent` sealed hierarchy (created, replaced, patched, deleted)
- `ScimEventPublisher` -- publish events after SCIM operations
- `ScimMetrics` / `ScimTracer` -- observability hooks

## Dependencies
- `kotlin-stdlib`, `kotlin-reflect`
- `jackson-databind`, `jackson-module-kotlin`, `jackson-datatype-jsr310` (all optional)
- `slf4j-api`

No Spring, no HTTP framework, no database dependencies.
