# SCIM 2.0 SDK :: Client :: Java HttpClient

`HttpTransport` adapter using Java's built-in `java.net.http.HttpClient` (Java 11+). This is the default transport -- zero additional dependencies.

## Usage

### Kotlin
```kotlin
val transport = HttpClientTransport()
// or with custom HttpClient:
val transport = HttpClientTransport(
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
)

val client = ScimClientBuilder()
    .transport(transport)
    // ...
    .build()
```

### Java
```java
var transport = new HttpClientTransport();
// or with custom HttpClient:
var transport = new HttpClientTransport(
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
);

ScimClient client = new ScimClientBuilder()
    .transport(transport)
    // ...
    .build();
```
