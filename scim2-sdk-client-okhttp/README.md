# SCIM 2.0 SDK :: Client :: OkHttp

`HttpTransport` adapter using [OkHttp](https://square.github.io/okhttp/). Use this when you need OkHttp features like connection pooling, interceptors, or WebSocket support.

## Usage
```kotlin
val transport = OkHttpTransport()
// or with custom OkHttpClient:
val transport = OkHttpTransport(
    OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .addInterceptor(loggingInterceptor)
        .build()
)

val client = ScimClientBuilder()
    .transport(transport)
    // ...
    .build()
```

## Dependencies
- `scim2-sdk-client`
- `com.squareup.okhttp3:okhttp`
