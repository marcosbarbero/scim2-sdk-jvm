# SCIM 2.0 SDK :: Client :: Java HttpClient

`HttpTransport` adapter using Java's built-in `java.net.http.HttpClient` (Java 11+). This is the default transport -- zero additional dependencies.

## Usage
```kotlin
val transport = JavaHttpClientTransport()
// or with custom HttpClient:
val transport = JavaHttpClientTransport(
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
)

val client = ScimClientBuilder()
    .transport(transport)
    // ...
    .build()
```
