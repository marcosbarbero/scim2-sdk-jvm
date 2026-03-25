# SCIM 2.0 SDK :: Server

This module provides the **SCIM Service Provider** framework -- the server-side implementation that receives and processes identity provisioning requests from Identity Providers (IdPs).

## What is a SCIM Service Provider?

When organizations use IdPs like **Okta**, **Azure AD**, **Keycloak**, or **PingFederate**, those IdPs need to push user/group changes to your application. The SCIM 2.0 protocol (RFC 7644) standardizes this:

```
+----------------+    SCIM 2.0 Protocol    +-----------------------+
|  Identity      |  ---- POST /Users --->  |  Your Application     |
|  Provider      |  ---- PATCH /Users -->  |  (SCIM Service        |
|  (Okta,        |  ---- DELETE /Users ->  |   Provider, using     |
|   Azure AD)    |  <--- 201 Created ----  |   this SDK)           |
+----------------+                          +-----------------------+
```

For example, when a new employee joins and is added in Okta, Okta sends a `POST /scim/v2/Users` request to your application. This module handles that request.

See: [Okta SCIM Integration Guide](https://help.okta.com/en-us/content/topics/apps/apps_app_integration_wizard_scim.htm)

## Architecture (Hexagonal)

```
+------------------- Inbound -------------------+
|  ScimController (Spring MVC)                   |
|  JDK HttpServer (plain Kotlin)                 |
|  Any HTTP framework                            |
+--------------------+---------------------------+
                     |
         +-----------v--------------+
         |  ScimEndpointDispatcher  | <-- Central routing engine
         |  (framework-agnostic)    |
         +-----------+--------------+
                     |
+--------------------+-------------------------+
|  Inbound Ports     |     Outbound Ports       |
|  ResourceHandler   |     ResourceRepository   |
|  BulkHandler       |     IdentityResolver     |
|  MeHandler         |     AuthorizationEval.   |
|                    |     ScimEventPublisher    |
|                    |     ScimOutboxPort        |
+--------------------+-------------------------+
                     |
+--------------------v-------------------------+
|  Your Implementation                          |
|  (JPA, MongoDB, DynamoDB, custom...)          |
+-----------------------------------------------+
```

## Key Interfaces

### ResourceHandler<T> (you implement this)
Handles CRUD + search for a specific resource type:
```kotlin
class MyUserHandler : ResourceHandler<User> {
    override val resourceType = User::class.java
    override val endpoint = "/Users"
    override fun create(resource: User, context: ScimRequestContext): User { ... }
    override fun get(id: ResourceId, context: ScimRequestContext): User { ... }
    // ... replace, patch, delete, search
}
```

### ResourceRepository<T> (persistence abstraction)
Generic persistence port -- no database opinion:
```kotlin
class MyUserRepository : ResourceRepository<User> {
    override fun create(resource: User): User { ... }
    override fun findById(id: String): User? { ... }
    // ... replace, delete, search
}
```

### IdentityResolver (authentication)
Extracts the authenticated identity from the incoming request:
```kotlin
class MyIdentityResolver : IdentityResolver {
    override fun resolve(request: ScimHttpRequest): ScimRequestContext { ... }
}
```

### ScimEndpointDispatcher
Routes all SCIM HTTP requests to the appropriate handler. Supports all RFC 7644 endpoints:
- `POST /Users`, `GET /Users/{id}`, `PUT`, `PATCH`, `DELETE`
- `GET /Users?filter=...`, `POST /Users/.search`
- `POST /Bulk`
- `GET /ServiceProviderConfig`, `/Schemas`, `/ResourceTypes`
- `GET|PUT|PATCH|DELETE /Me`
- Case-insensitive routing (`/users` = `/Users`)

## Dependencies
- `scim2-sdk-core`
- No Spring, no HTTP framework, no database
