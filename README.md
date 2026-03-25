# SCIM 2.0 SDK for JVM

A modern, Kotlin-first SCIM 2.0 (RFC 7643/7644) SDK for the JVM with full Java interop.

## Features

- **Complete RFC compliance**: All SCIM 2.0 operations (CRUD, search, bulk, patch, discovery)
- **Kotlin-first with Java interop**: Kotlin DSLs, extension functions, coroutines, plus @Jvm annotations for Java users
- **Hexagonal architecture**: Framework-agnostic core, pluggable persistence and identity
- **Spring Boot starter**: Auto-configuration with sensible defaults
- **Works without Spring**: Use with any JVM HTTP framework
- **OOTB persistence**: JPA adapter with reference schemas for PostgreSQL, MySQL, Oracle, MSSQL, H2
- **Observability**: Metrics (Micrometer), tracing, structured logging, event system
- **Type-safe client**: Fluent API with Kotlin DSLs for filters, patches, searches
- **RFC 9457 ProblemDetail**: Content-negotiated error responses
- **Extensible**: SPI for serialization, HTTP transport, identity, authorization, events

## Quick Start

### With Spring Boot

Add the starter dependency:

```xml
<dependency>
    <groupId>com.marcosbarbero</groupId>
    <artifactId>scim2-sdk-spring-boot-starter</artifactId>
    <version>${scim2-sdk.version}</version>
</dependency>
```

Configure in `application.yml`:

```yaml
scim:
  base-path: /scim/v2
  persistence:
    enabled: true  # enables JPA-backed storage
  bulk:
    enabled: true
  filter:
    enabled: true
```

That's it! The starter auto-configures:
- SCIM endpoints at `/scim/v2/*` (Users, Groups, Schemas, ResourceTypes, ServiceProviderConfig, Bulk)
- JPA persistence with H2/PostgreSQL/MySQL/Oracle/MSSQL
- Jackson serialization with SCIM module
- Micrometer metrics (when on classpath)
- RFC 9457 ProblemDetail error responses

To provide your own `ResourceHandler`:

```kotlin
@Component
class CustomUserHandler(private val userService: UserService) : ResourceHandler<User> {
    override val resourceType = User::class.java
    override val endpoint = "/Users"

    override fun create(resource: User, context: ScimRequestContext) = userService.create(resource)
    override fun get(id: ResourceId, context: ScimRequestContext) = userService.findById(id.value)
    // ... other methods
}
```

### Without Spring Boot

```kotlin
fun main() {
    // Create handlers
    val userHandler = InMemoryResourceHandler(User::class.java, "/Users", userRepository)
    val groupHandler = InMemoryResourceHandler(Group::class.java, "/Groups", groupRepository)

    // Create infrastructure
    val schemaRegistry = SchemaRegistry().apply {
        register(User::class)
        register(Group::class)
    }
    val serializer = JacksonScimSerializer()
    val config = ScimServerConfig(basePath = "/scim/v2")
    val discoveryService = DiscoveryService(listOf(userHandler, groupHandler), schemaRegistry, config)

    // Create dispatcher
    val dispatcher = ScimEndpointDispatcher(
        handlers = listOf(userHandler, groupHandler),
        discoveryService = discoveryService,
        config = config,
        serializer = serializer,
        bulkHandler = null,
        meHandler = null
    )

    // Use with any HTTP server - dispatch(ScimHttpRequest) returns ScimHttpResponse
    val response = dispatcher.dispatch(scimRequest)
}
```

See [sample-server-plain](scim2-sdk-samples/sample-server-plain/) for a complete example using JDK's built-in HTTP server.

### Client Usage

```kotlin
// Create client
val client = ScimClientBuilder()
    .baseUrl("https://scim.example.com/scim/v2")
    .transport(JavaHttpClientTransport())
    .serializer(JacksonScimSerializer())
    .authentication(BearerTokenAuthentication("your-token"))
    .build()

// Create a user
val user = User(userName = "john.doe", displayName = "John Doe")
val response = client.create("/Users", user, User::class)

// Search with SearchRequest
val searchRequest = SearchRequest(
    filter = "userName sw \"john\"",
    sortBy = "userName",
    count = 25
)
val results = client.search("/Users", searchRequest, User::class)

// Patch with PatchRequest
val patch = PatchRequest(
    operations = listOf(
        PatchOperation(op = PatchOp.REPLACE, path = "displayName", value = TextNode("John D. Doe")),
        PatchOperation(op = PatchOp.REMOVE, path = "nickName")
    )
)
client.patch("/Users", userId, patch, User::class)
```

## Modules

| Module | Description |
|---|---|
| `scim2-sdk-core` | Domain model, filter/path parsing, PATCH engine, serialization SPI |
| `scim2-sdk-server` | Server framework with hexagonal architecture (ports + adapters) |
| `scim2-sdk-client` | Fluent client API with Kotlin DSLs |
| `scim2-sdk-client-httpclient` | Java HttpClient transport adapter |
| `scim2-sdk-client-okhttp` | OkHttp transport adapter |
| `scim2-sdk-spring-boot-autoconfigure` | Spring Boot auto-configuration |
| `scim2-sdk-spring-boot-starter` | Spring Boot starter (aggregates dependencies) |
| `scim2-sdk-test` | Test fixtures, contract tests, in-memory server |

## Configuration Properties

All properties are optional with sensible defaults:

| Property | Default | Description |
|---|---|---|
| `scim.base-path` | `/scim/v2` | Base path for SCIM endpoints |
| `scim.persistence.enabled` | `false` | Enable JPA-backed persistence |
| `scim.persistence.table-name` | `scim_resources` | Database table name |
| `scim.persistence.schema-name` | (none) | Database schema name |
| `scim.bulk.enabled` | `true` | Enable bulk operations |
| `scim.bulk.max-operations` | `1000` | Max operations per bulk request |
| `scim.filter.enabled` | `true` | Enable filtering |
| `scim.filter.max-results` | `200` | Max results per filtered query |
| `scim.etag.enabled` | `true` | Enable ETag support |
| `scim.patch.enabled` | `true` | Enable PATCH operations |
| `scim.sort.enabled` | `false` | Enable sorting |
| `scim.client.base-url` | (none) | SCIM client target URL |

## Database Support

When `scim.persistence.enabled=true`, the JPA adapter stores resources as JSON. Reference schemas are provided for:
- PostgreSQL, MySQL, Oracle, MS SQL Server, H2

Find them in `scim2-sdk-spring-boot-autoconfigure/src/main/resources/db/scim/`.

## Requirements

- Java 25+
- Maven 3.9+
- Spring Boot 3.4+ (for starter, optional)

## License

[Apache License 2.0](LICENSE)
