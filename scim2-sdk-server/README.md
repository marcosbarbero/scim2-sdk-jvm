# SCIM 2.0 SDK :: Server

This module provides the **SCIM Service Provider** framework -- the server-side implementation that receives and processes identity provisioning requests from Identity Providers (IdPs).

## What is a SCIM Service Provider?

When organizations use IdPs like **Okta**, **Azure AD**, **Keycloak**, or **PingFederate**, those IdPs need to push user/group changes to your application. The SCIM 2.0 protocol (RFC 7644) standardizes this:

```mermaid
sequenceDiagram
    participant IdP as Identity Provider<br/>(Okta, Azure AD, Keycloak)
    participant App as Your Application<br/>(SCIM Service Provider)
    participant DB as Your Database

    IdP->>App: POST /scim/v2/Users (new employee)
    App->>DB: INSERT user
    App-->>IdP: 201 Created

    IdP->>App: PATCH /scim/v2/Users/{id} (role change)
    App->>DB: UPDATE user
    App-->>IdP: 200 OK

    IdP->>App: DELETE /scim/v2/Users/{id} (offboarding)
    App->>DB: DELETE user
    App-->>IdP: 204 No Content
```

For example, when a new employee joins and is added in Okta, Okta sends a `POST /scim/v2/Users` request to your application. This module handles that request.

See: [Okta SCIM Integration Guide](https://help.okta.com/en-us/content/topics/apps/apps_app_integration_wizard_scim.htm)

## Architecture (Hexagonal)

```mermaid
graph TD
    subgraph "Inbound Adapters"
        SC[ScimController<br/>Spring MVC]
        JDK[JDK HttpServer<br/>Plain Kotlin]
    end

    subgraph "Core Engine"
        D[ScimEndpointDispatcher]
    end

    subgraph "Inbound Ports"
        RH[ResourceHandler]
        BH[BulkHandler]
        MH[MeHandler]
    end

    subgraph "Outbound Ports"
        RR[ResourceRepository]
        IR[IdentityResolver]
        AE[AuthorizationEvaluator]
        EP[ScimEventPublisher]
    end

    subgraph "Outbound Adapters<br/>(you provide or use OOTB)"
        JPA[JPA Repository]
        KC[Keycloak Resolver]
        CUSTOM[Your Custom Impl]
    end

    SC --> D
    JDK --> D
    D --> RH & BH & MH
    RH --> RR
    D --> IR & AE & EP
    RR --> JPA & CUSTOM
    IR --> KC

    style D fill:#4CAF50,color:#fff
```

## Key Interfaces

### ResourceHandler<T> (you implement this)
Handles CRUD + search for a specific resource type:

#### Kotlin
```kotlin
class MyUserHandler : ResourceHandler<User> {
    override val resourceType = User::class.java
    override val endpoint = "/Users"
    override fun create(resource: User, context: ScimRequestContext): User { ... }
    override fun get(id: ResourceId, context: ScimRequestContext): User { ... }
    // ... replace, patch, delete, search
}
```

#### Java
```java
public class MyUserHandler implements ResourceHandler<User> {
    @Override public Class<User> getResourceType() { return User.class; }
    @Override public String getEndpoint() { return "/Users"; }
    @Override public User create(User resource, ScimRequestContext context) { ... }
    @Override public User get(ResourceId id, ScimRequestContext context) { ... }
    // ... replace, patch, delete, search
}
```

### ResourceRepository<T> (persistence abstraction)
Generic persistence port -- no database opinion:

#### Kotlin
```kotlin
class MyUserRepository : ResourceRepository<User> {
    override fun create(resource: User): User { ... }
    override fun findById(id: String): User? { ... }
    // ... replace, delete, search
}
```

#### Java
```java
public class MyUserRepository implements ResourceRepository<User> {
    @Override public User create(User resource) { ... }
    @Override public User findById(String id) { ... }
    // ... replace, delete, search
}
```

### IdentityResolver (authentication)
Extracts the authenticated identity from the incoming request:

#### Kotlin
```kotlin
class MyIdentityResolver : IdentityResolver {
    override fun resolve(request: ScimHttpRequest): ScimRequestContext { ... }
}
```

#### Java
```java
public class MyIdentityResolver implements IdentityResolver {
    @Override public ScimRequestContext resolve(ScimHttpRequest request) { ... }
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
