# SCIM 2.0 SDK :: Test

Test utilities, contract test base classes, and in-memory implementations for testing SCIM integrations.

## In-Memory Implementations

### InMemoryResourceRepository
A `ConcurrentHashMap`-backed `ResourceRepository<T>` for testing:
```kotlin
val repo = InMemoryResourceRepository<User>()
```

### InMemoryResourceHandler
A `ResourceHandler<T>` backed by `InMemoryResourceRepository`:
```kotlin
val handler = InMemoryResourceHandler(User::class.java, "/Users", repo)
```

### InMemoryScimServer
A complete SCIM server for integration testing:
```kotlin
val server = InMemoryScimServer()
server.createUser(User(userName = "test"))
val response = server.dispatch(ScimHttpRequest(GET, "/scim/v2/Users"))
```

## Contract Tests

### ResourceHandlerContractTest
Abstract base class with 23 tests. Extend it to prove your `ResourceHandler` implementation is correct:
```kotlin
class MyUserHandlerTest : ResourceHandlerContractTest<User>() {
    override fun createHandler() = MyUserHandler()
    override fun sampleResource() = User(userName = "test")
    override fun modifiedResource(original: User) = original.copy(displayName = "Modified")
}
```

### ScimApiContractTest
Abstract base class with 19 HTTP-level tests validating RFC 7644/7643 compliance. Each test references the specific RFC section:
```kotlin
class MyApiContractTest : ScimApiContractTest() {
    override fun createDispatcher() = myDispatcher
    override fun sampleUserJson() = serializer.serialize(User(userName = "test"))
}
```

Tests include:
- `POST returns 201 Created (RFC 7644 S3.1)`
- `POST response includes Location header (RFC 7644 S3.1)`
- `GET returns ETag header (RFC 7644 S3.14)`
- `GET with If-None-Match returns 304 (RFC 7644 S3.14)`
- `DELETE returns 204 No Content (RFC 7644 S3.6)`
- ... and more

## ArchUnit Tests
Validates architectural boundaries:
- Core module has no Spring/server/client dependencies
- Server module has no client/Spring dependencies
