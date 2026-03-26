# Architecture

## Module Dependency Graph

```mermaid
graph TD
    BOM[scim2-sdk-bom]
    CORE[scim2-sdk-core<br/>Domain model, filter parser,<br/>PATCH engine, serialization SPI]
    SERVER[scim2-sdk-server<br/>Dispatcher, ports,<br/>engines, interceptors]
    CLIENT[scim2-sdk-client<br/>ScimClient, DSLs,<br/>HttpTransport SPI]
    HTTPCLIENT[scim2-sdk-client-httpclient<br/>Java HttpClient adapter]
    OKHTTP[scim2-sdk-client-okhttp<br/>OkHttp adapter]
    AUTOCONFIG[scim2-sdk-spring-boot-autoconfigure<br/>Auto-configuration, JPA adapter,<br/>Micrometer, ProblemDetail]
    STARTER[scim2-sdk-spring-boot-starter<br/>Empty JAR, aggregates deps]
    TEST[scim2-sdk-test<br/>InMemoryScimServer,<br/>contract tests, ArchUnit]

    SERVER --> CORE
    CLIENT --> CORE
    HTTPCLIENT --> CLIENT
    OKHTTP --> CLIENT
    AUTOCONFIG -.optional.-> SERVER
    AUTOCONFIG -.optional.-> CLIENT
    AUTOCONFIG -.optional.-> HTTPCLIENT
    STARTER --> AUTOCONFIG
    STARTER --> SERVER
    STARTER --> CLIENT
    STARTER --> HTTPCLIENT
    TEST --> CORE
    TEST --> SERVER
    TEST --> CLIENT

    style CORE fill:#4CAF50,color:#fff
    style SERVER fill:#2196F3,color:#fff
    style CLIENT fill:#FF9800,color:#fff
    style AUTOCONFIG fill:#9C27B0,color:#fff
    style STARTER fill:#9C27B0,color:#fff
    style TEST fill:#607D8B,color:#fff
```

## Request Processing Flow

```mermaid
sequenceDiagram
    participant C as SCIM Client
    participant SC as ScimController<br/>(Spring MVC)
    participant I as ScimInterceptor Chain
    participant D as ScimEndpointDispatcher
    participant IR as IdentityResolver
    participant AE as AuthorizationEvaluator
    participant H as ResourceHandler
    participant R as ResourceRepository
    participant DB as Database
    participant EP as ScimEventPublisher
    participant M as ScimMetrics

    C->>SC: HTTP Request
    SC->>D: dispatch(ScimHttpRequest)
    D->>IR: resolve(request)
    IR-->>D: ScimRequestContext (principal, roles)
    D->>M: incrementActiveRequests()
    D->>I: preHandle(request, context)
    I-->>D: processed request
    D->>AE: canCreate/canRead/canUpdate/canDelete()
    AE-->>D: authorized ✓
    D->>H: create/get/replace/patch/delete/search()
    H->>R: save/findById/replace/delete/search()
    R->>DB: SQL query
    DB-->>R: result
    R-->>H: resource
    H-->>D: resource
    D->>EP: publish(ResourceCreatedEvent)
    D->>I: postHandle(request, response, context)
    D->>M: recordOperation(duration)
    D->>M: decrementActiveRequests()
    D-->>SC: ScimHttpResponse
    SC-->>C: HTTP Response
```

## Authentication with Keycloak (IdP)

```mermaid
sequenceDiagram
    participant IdP as Identity Provider<br/>(Keycloak/Okta/Azure AD)
    participant C as SCIM Client
    participant S as SCIM Server
    participant IR as IdentityResolver
    participant H as ResourceHandler

    Note over C,IdP: Step 1: Obtain Access Token
    C->>IdP: POST /token<br/>(client_credentials grant)
    IdP-->>C: JWT access_token

    Note over C,S: Step 2: SCIM Operations with JWT
    C->>S: POST /scim/v2/Users<br/>Authorization: Bearer {token}
    S->>IR: resolve(request)<br/>Validates JWT signature + claims
    IR->>IdP: JWKS endpoint (cached)
    IdP-->>IR: Public keys
    IR-->>S: ScimRequestContext<br/>(principal, roles, claims)
    S->>H: create(user, context)
    H-->>S: created user (with id, meta)
    S-->>C: 201 Created + Location header

    Note over C,S: Step 3: Unauthenticated = Rejected
    C->>S: GET /scim/v2/Users<br/>(no Authorization header)
    S-->>C: 401 Unauthorized
```

## Outbox Pattern for Reliable Event Publishing

```mermaid
sequenceDiagram
    participant C as SCIM Client<br/>(Azure AD/Okta)
    participant D as ScimEndpointDispatcher
    participant H as ResourceHandler
    participant DB as Database
    participant OP as Outbox Adapter
    participant P as Outbox Poller<br/>(async)
    participant K as Message Broker<br/>(Kafka/SQS)
    participant DS as Downstream<br/>Consumers

    C->>D: POST /scim/v2/Users
    D->>H: create(user, context)

    rect rgb(200, 230, 200)
        Note over H,DB: Same Database Transaction
        H->>DB: INSERT INTO users (...)
        H->>OP: store(ResourceCreatedEvent)
        OP->>DB: INSERT INTO scim_outbox (...)
        Note over DB: COMMIT (atomic)
    end

    H-->>D: created user
    D-->>C: 201 Created

    Note over P,DS: Asynchronous Processing
    P->>DB: SELECT FROM scim_outbox<br/>WHERE processed = false
    DB-->>P: pending events
    P->>K: publish(ResourceCreatedEvent)
    K-->>DS: consume event
    P->>DB: UPDATE scim_outbox<br/>SET processed = true
```

## SCIM Provisioning Lifecycle

```mermaid
sequenceDiagram
    participant IdP as Identity Provider
    participant S as SCIM Service Provider

    Note over IdP,S: Discovery Phase
    IdP->>S: GET /ServiceProviderConfig
    S-->>IdP: Supported features (patch, bulk, filter, etag)
    IdP->>S: GET /Schemas
    S-->>IdP: User, Group, EnterpriseUser schemas
    IdP->>S: GET /ResourceTypes
    S-->>IdP: User, Group resource types

    Note over IdP,S: Provisioning Phase
    IdP->>S: POST /Users (create user)
    S-->>IdP: 201 + user with id

    IdP->>S: GET /Users?filter=userName eq "john"
    S-->>IdP: ListResponse with matching users

    IdP->>S: PATCH /Users/{id} (update attributes)
    S-->>IdP: 200 + updated user

    IdP->>S: PUT /Users/{id} (full replace)
    S-->>IdP: 200 + replaced user

    Note over IdP,S: Deprovisioning
    IdP->>S: PATCH /Users/{id}<br/>op: replace, path: active, value: false
    S-->>IdP: 200 + deactivated user

    IdP->>S: DELETE /Users/{id}
    S-->>IdP: 204 No Content
```

## Hexagonal Architecture (Server Module)

```mermaid
graph TD
    subgraph "Inbound Adapters"
        SC[ScimController<br/>Spring MVC]
        JDK[JDK HttpServer<br/>Plain Kotlin]
    end

    subgraph "Inbound Ports"
        RH[ResourceHandler&lt;T&gt;]
        BH[BulkHandler]
        MH[MeHandler]
    end

    subgraph "Domain / Engine"
        D[ScimEndpointDispatcher]
        PE[PatchEngine]
        AP[AttributeProjector]
        EE[ETagEngine]
        PG[PaginationEngine]
        BE[BulkEngine]
        DS[DiscoveryService]
    end

    subgraph "Outbound Ports"
        RR[ResourceRepository&lt;T&gt;]
        IR[IdentityResolver]
        AE[AuthorizationEvaluator]
        EP[ScimEventPublisher]
    end

    subgraph "Outbound Adapters"
        JPA[JpaResourceRepository<br/>Spring Data JPA]
        IMR[InMemoryResourceRepository<br/>Test]
        KC[KeycloakIdentityResolver]
        OK[OktaIdentityResolver]
        NE[NoOpEventPublisher]
        SE[SpringEventPublisher]
        NO[NamastackOutboxAdapter]
    end

    SC --> D
    JDK --> D
    D --> RH
    D --> BH
    D --> MH
    D --> PE
    D --> AP
    D --> EE
    D --> PG
    D --> BE
    D --> DS
    D --> IR
    D --> AE
    D --> EP
    RH --> RR
    RR --> JPA
    RR --> IMR
    IR --> KC
    IR --> OK
    EP --> NE
    EP --> SE
    OP --> NO

    style D fill:#4CAF50,color:#fff
    style RH fill:#2196F3,color:#fff
    style RR fill:#2196F3,color:#fff
    style JPA fill:#9C27B0,color:#fff
    style KC fill:#FF9800,color:#fff
```
