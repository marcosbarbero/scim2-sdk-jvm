# SCIM 2.0 HTTP API Reference

Complete HTTP request/response examples for every SCIM 2.0 endpoint. All examples assume the SCIM server runs at `https://your-app.com/scim/v2`.

> **RFC References:**
> - [RFC 7643 — SCIM Core Schema](https://www.rfc-editor.org/rfc/rfc7643)
> - [RFC 7644 — SCIM Protocol](https://www.rfc-editor.org/rfc/rfc7644)
> - [RFC 9457 — Problem Details](https://www.rfc-editor.org/rfc/rfc9457)

---

## Discovery Endpoints

### GET /ServiceProviderConfig — [RFC 7644 §4](https://www.rfc-editor.org/rfc/rfc7644#section-4)

Returns the service provider's configuration, including which optional features are supported.

```bash
curl -s https://your-app.com/scim/v2/ServiceProviderConfig \
  -H "Authorization: Bearer {token}"
```

**Response: 200 OK**
```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"],
  "patch": { "supported": true },
  "bulk": {
    "supported": true,
    "maxOperations": 1000,
    "maxPayloadSize": 1048576
  },
  "filter": {
    "supported": true,
    "maxResults": 200
  },
  "changePassword": { "supported": false },
  "sort": { "supported": false },
  "etag": { "supported": true },
  "authenticationSchemes": [
    {
      "type": "oauthbearertoken",
      "name": "OAuth Bearer Token",
      "description": "Authentication scheme using OAuth 2.0 Bearer Token (RFC 6750)"
    }
  ]
}
```

### GET /Schemas — [RFC 7644 §4](https://www.rfc-editor.org/rfc/rfc7644#section-4)

Returns all resource schemas supported by the server.

```bash
curl -s https://your-app.com/scim/v2/Schemas \
  -H "Authorization: Bearer {token}"
```

**Response: 200 OK**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "totalResults": 2,
  "startIndex": 1,
  "itemsPerPage": 2,
  "Resources": [
    {
      "id": "urn:ietf:params:scim:schemas:core:2.0:User",
      "name": "User",
      "description": "User resource",
      "attributes": [
        {
          "name": "userName",
          "type": "string",
          "required": true,
          "mutability": "readWrite",
          "returned": "default",
          "uniqueness": "server"
        }
      ]
    }
  ]
}
```

### GET /ResourceTypes — [RFC 7644 §4](https://www.rfc-editor.org/rfc/rfc7644#section-4)

Returns the types of resources available.

```bash
curl -s https://your-app.com/scim/v2/ResourceTypes \
  -H "Authorization: Bearer {token}"
```

**Response: 200 OK**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "totalResults": 2,
  "Resources": [
    {
      "id": "User",
      "name": "User",
      "endpoint": "/Users",
      "schema": "urn:ietf:params:scim:schemas:core:2.0:User",
      "schemaExtensions": [
        {
          "schema": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
          "required": false
        }
      ]
    },
    {
      "id": "Group",
      "name": "Group",
      "endpoint": "/Groups",
      "schema": "urn:ietf:params:scim:schemas:core:2.0:Group"
    }
  ]
}
```

---

## User Endpoints

### Create User — POST /Users — [RFC 7644 §3.1](https://www.rfc-editor.org/rfc/rfc7644#section-3.1)

```bash
curl -X POST https://your-app.com/scim/v2/Users \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "jane.doe",
    "name": {
      "givenName": "Jane",
      "familyName": "Doe",
      "formatted": "Jane Doe"
    },
    "displayName": "Jane Doe",
    "emails": [
      { "value": "jane.doe@example.com", "type": "work", "primary": true }
    ],
    "phoneNumbers": [
      { "value": "+1-555-0100", "type": "work" }
    ],
    "active": true
  }'
```

**Response: 201 Created**
```http
HTTP/1.1 201 Created
Content-Type: application/scim+json
Location: /scim/v2/Users/2819c223-7f76-453a-919d-413861904646
ETag: W/"1"
```
```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "2819c223-7f76-453a-919d-413861904646",
  "userName": "jane.doe",
  "name": {
    "givenName": "Jane",
    "familyName": "Doe",
    "formatted": "Jane Doe"
  },
  "displayName": "Jane Doe",
  "emails": [
    { "value": "jane.doe@example.com", "type": "work", "primary": true }
  ],
  "phoneNumbers": [
    { "value": "+1-555-0100", "type": "work" }
  ],
  "active": true,
  "meta": {
    "resourceType": "User",
    "created": "2026-03-25T10:00:00Z",
    "lastModified": "2026-03-25T10:00:00Z",
    "location": "/scim/v2/Users/2819c223-7f76-453a-919d-413861904646",
    "version": "W/\"1\""
  }
}
```

### Get User — GET /Users/{id} — [RFC 7644 §3.2](https://www.rfc-editor.org/rfc/rfc7644#section-3.2)

```bash
curl -s https://your-app.com/scim/v2/Users/2819c223-7f76-453a-919d-413861904646 \
  -H "Authorization: Bearer {token}"
```

**Response: 200 OK** (same body as create response)

### Get User with ETag — [RFC 7644 §3.14](https://www.rfc-editor.org/rfc/rfc7644#section-3.14)

```bash
# Conditional GET — returns 304 if not modified
curl -s https://your-app.com/scim/v2/Users/2819c223-7f76-453a-919d-413861904646 \
  -H "Authorization: Bearer {token}" \
  -H 'If-None-Match: W/"1"'
```

**Response: 304 Not Modified** (no body)

### Replace User — PUT /Users/{id} — [RFC 7644 §3.3](https://www.rfc-editor.org/rfc/rfc7644#section-3.3)

Replaces the entire user resource. Attributes not present in the request are cleared.

```bash
curl -X PUT https://your-app.com/scim/v2/Users/2819c223-7f76-453a-919d-413861904646 \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -H 'If-Match: W/"1"' \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "jane.doe",
    "displayName": "Jane D. Doe",
    "active": true
  }'
```

**Response: 200 OK** with updated user (ETag incremented to `W/"2"`)

### Patch User — PATCH /Users/{id} — [RFC 7644 §3.5.2](https://www.rfc-editor.org/rfc/rfc7644#section-3.5.2)

Partially updates a user. Supports `add`, `remove`, `replace` operations.

```bash
# Replace display name and add an email
curl -X PATCH https://your-app.com/scim/v2/Users/2819c223-7f76-453a-919d-413861904646 \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
    "Operations": [
      { "op": "replace", "path": "displayName", "value": "Jane D. Doe" },
      { "op": "add", "path": "emails", "value": [{ "value": "jane@personal.com", "type": "home" }] },
      { "op": "remove", "path": "phoneNumbers[type eq \"work\"]" }
    ]
  }'
```

**Response: 200 OK** with updated user

### Deactivate User — PATCH /Users/{id} — [RFC 7644 §3.5.2](https://www.rfc-editor.org/rfc/rfc7644#section-3.5.2)

Common pattern used by IdPs to deactivate rather than delete:

```bash
curl -X PATCH https://your-app.com/scim/v2/Users/2819c223-7f76-453a-919d-413861904646 \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
    "Operations": [
      { "op": "replace", "path": "active", "value": false }
    ]
  }'
```

### Delete User — DELETE /Users/{id} — [RFC 7644 §3.6](https://www.rfc-editor.org/rfc/rfc7644#section-3.6)

```bash
curl -X DELETE https://your-app.com/scim/v2/Users/2819c223-7f76-453a-919d-413861904646 \
  -H "Authorization: Bearer {token}" \
  -H 'If-Match: W/"2"'
```

**Response: 204 No Content**

### Search Users (GET) — [RFC 7644 §3.4.2](https://www.rfc-editor.org/rfc/rfc7644#section-3.4.2)

```bash
# Search with filter, pagination, and sorting
curl -s "https://your-app.com/scim/v2/Users?filter=userName%20sw%20%22jane%22&startIndex=1&count=10&sortBy=userName&sortOrder=ascending" \
  -H "Authorization: Bearer {token}"
```

**Response: 200 OK**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "totalResults": 1,
  "startIndex": 1,
  "itemsPerPage": 1,
  "Resources": [
    {
      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
      "id": "2819c223-7f76-453a-919d-413861904646",
      "userName": "jane.doe",
      "displayName": "Jane Doe"
    }
  ]
}
```

### Search Users (POST) — [RFC 7644 §3.4.3](https://www.rfc-editor.org/rfc/rfc7644#section-3.4.3)

Use POST when the filter is too long for a URL:

```bash
curl -X POST https://your-app.com/scim/v2/Users/.search \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:SearchRequest"],
    "filter": "userName sw \"jane\" and active eq true",
    "startIndex": 1,
    "count": 10,
    "sortBy": "userName",
    "sortOrder": "ascending"
  }'
```

---

## Group Endpoints

### Create Group — POST /Groups — [RFC 7644 §3.1](https://www.rfc-editor.org/rfc/rfc7644#section-3.1)

```bash
curl -X POST https://your-app.com/scim/v2/Groups \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
    "displayName": "Engineering",
    "members": [
      { "value": "2819c223-7f76-453a-919d-413861904646", "display": "Jane Doe" }
    ]
  }'
```

**Response: 201 Created** with group resource including `id` and `meta`.

### Add Member to Group — PATCH /Groups/{id}

```bash
curl -X PATCH https://your-app.com/scim/v2/Groups/{group-id} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
    "Operations": [
      {
        "op": "add",
        "path": "members",
        "value": [{ "value": "user-id-here" }]
      }
    ]
  }'
```

### Remove Member from Group — PATCH /Groups/{id}

```bash
curl -X PATCH https://your-app.com/scim/v2/Groups/{group-id} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
    "Operations": [
      {
        "op": "remove",
        "path": "members[value eq \"user-id-here\"]"
      }
    ]
  }'
```

---

## Bulk Operations — [RFC 7644 §3.7](https://www.rfc-editor.org/rfc/rfc7644#section-3.7)

```bash
curl -X POST https://your-app.com/scim/v2/Bulk \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkRequest"],
    "failOnErrors": 2,
    "Operations": [
      {
        "method": "POST",
        "path": "/Users",
        "bulkId": "user1",
        "data": {
          "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
          "userName": "alice"
        }
      },
      {
        "method": "POST",
        "path": "/Groups",
        "bulkId": "group1",
        "data": {
          "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
          "displayName": "Team Alpha",
          "members": [{ "value": "bulkId:user1" }]
        }
      }
    ]
  }'
```

**Response: 200 OK**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkResponse"],
  "Operations": [
    { "method": "POST", "bulkId": "user1", "status": "201", "location": "/scim/v2/Users/abc-123" },
    { "method": "POST", "bulkId": "group1", "status": "201", "location": "/scim/v2/Groups/def-456" }
  ]
}
```

Note: `bulkId:user1` in the group's members is resolved to the actual ID assigned to alice.

---

## Error Responses

### SCIM Error Format — [RFC 7644 §3.12](https://www.rfc-editor.org/rfc/rfc7644#section-3.12)

Default error format when client sends `Accept: application/scim+json` (or no Accept header):

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
  "status": "404",
  "scimType": null,
  "detail": "Resource 2819c223 not found"
}
```

### RFC 9457 ProblemDetail Format — [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)

When client sends `Accept: application/problem+json`:

```bash
curl -s https://your-app.com/scim/v2/Users/nonexistent \
  -H "Authorization: Bearer {token}" \
  -H "Accept: application/problem+json"
```

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Resource nonexistent not found"
}
```

### Common Error Status Codes

| Status | scimType | Meaning |
|---|---|---|
| 400 | `invalidFilter` | Invalid filter syntax |
| 400 | `invalidSyntax` | Malformed request body |
| 400 | `invalidPath` | Invalid PATCH path |
| 400 | `invalidValue` | Value violates schema |
| 400 | `mutability` | Attempted to modify readOnly attribute |
| 401 | -- | Missing or invalid authentication |
| 403 | -- | Insufficient permissions |
| 404 | -- | Resource not found |
| 409 | `uniqueness` | Duplicate resource (e.g., duplicate userName) |
| 412 | -- | ETag precondition failed (stale If-Match) |
| 413 | -- | Bulk payload too large |
| 500 | -- | Internal server error |
| 501 | -- | Feature not supported (e.g., bulk disabled) |
