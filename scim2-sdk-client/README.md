# SCIM 2.0 SDK :: Client

Type-safe SCIM 2.0 client for consuming SCIM Service Provider APIs. Use this to provision users/groups programmatically.

## Usage

### Kotlin
```kotlin
val client = ScimClientBuilder()
    .baseUrl("https://scim.example.com/scim/v2")
    .transport(JavaHttpClientTransport())
    .serializer(JacksonScimSerializer())
    .authentication(BearerTokenAuthentication(token))
    .build()

// Type-safe operations
val user = client.createUser(User(userName = "john.doe")).value
val found = client.getUser(user.id!!).value
client.patchUser(user.id!!, scimPatch { replace("displayName", "John") })
val results = client.searchUsers("userName sw \"john\"")
client.deleteUser(user.id!!)
```

### Java
```java
ScimClient client = new ScimClientBuilder()
    .baseUrl("https://scim.example.com/scim/v2")
    .transport(new JavaHttpClientTransport())
    .serializer(new JacksonScimSerializer())
    .authentication(new BearerTokenAuthentication(token))
    .build();

ScimResponse<User> response = ScimClients.createUser(client, user);
ScimClients.deleteUser(client, id);
```

### Kotlin DSLs
```kotlin
// Filter DSL
val filter = scimFilter { userName eq "john" and active.pr }

// Patch DSL
val patch = scimPatch { replace("displayName", "John"); remove("nickName") }

// Search DSL
val request = scimSearch { filter { userName sw "j" }; sortBy("userName"); count(25) }
```

### Async (Coroutines)
```kotlin
val asyncClient = AsyncScimClient(client)
val user = asyncClient.createUser(user)
```

## Transport Adapters
The client uses an `HttpTransport` SPI. Two adapters are provided:
- `scim2-sdk-client-httpclient` -- Java 11+ HttpClient (default)
- `scim2-sdk-client-okhttp` -- OkHttp

## Dependencies
- `scim2-sdk-core`
- `kotlinx-coroutines-core` (for AsyncScimClient)
