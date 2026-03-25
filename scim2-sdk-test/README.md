# SCIM 2.0 SDK :: Test

Test utilities, contract test base classes, and in-memory implementations for testing SCIM integrations.

## In-Memory Implementations

### InMemoryResourceRepository
A `ConcurrentHashMap`-backed `ResourceRepository<T>` for testing:

#### Kotlin
```kotlin
val repo = InMemoryResourceRepository<User>()
```

#### Java
```java
var repo = new InMemoryResourceRepository<User>();
```

### InMemoryResourceHandler
A `ResourceHandler<T>` backed by `InMemoryResourceRepository`:

#### Kotlin
```kotlin
val handler = InMemoryResourceHandler(User::class.java, "/Users", repo)
```

#### Java
```java
var handler = new InMemoryResourceHandler<>(User.class, "/Users", repo);
```

### InMemoryScimServer
A complete SCIM server for integration testing:

#### Kotlin
```kotlin
val server = InMemoryScimServer()
server.createUser(User(userName = "test"))
val response = server.dispatch(ScimHttpRequest(GET, "/scim/v2/Users"))
```

#### Java
```java
var server = new InMemoryScimServer();
server.createUser(new User("test"));
var response = server.dispatch(new ScimHttpRequest(GET, "/scim/v2/Users"));
```

## Contract Tests

### ResourceHandlerContractTest
Abstract base class with 23 tests. Extend it to prove your `ResourceHandler` implementation is correct:

#### Kotlin
```kotlin
class MyUserHandlerTest : ResourceHandlerContractTest<User>() {
    override fun createHandler() = MyUserHandler()
    override fun sampleResource() = User(userName = "test")
    override fun modifiedResource(original: User) = original.copy(displayName = "Modified")
}
```

#### Java
```java
public class MyUserHandlerTest extends ResourceHandlerContractTest<User> {
    @Override protected ResourceHandler<User> createHandler() { return new MyUserHandler(); }
    @Override protected User sampleResource() { return new User("test"); }
    @Override protected User modifiedResource(User original) {
        return original.withDisplayName("Modified");
    }
}
```

### ScimApiContractTest
Abstract base class with 19 HTTP-level tests validating RFC 7644/7643 compliance. Each test references the specific RFC section:

#### Kotlin
```kotlin
class MyApiContractTest : ScimApiContractTest() {
    override fun createDispatcher() = myDispatcher
    override fun sampleUserJson() = serializer.serialize(User(userName = "test"))
}
```

#### Java
```java
public class MyApiContractTest extends ScimApiContractTest {
    @Override protected ScimEndpointDispatcher createDispatcher() { return myDispatcher; }
    @Override protected byte[] sampleUserJson() { return serializer.serialize(new User("test")); }
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
