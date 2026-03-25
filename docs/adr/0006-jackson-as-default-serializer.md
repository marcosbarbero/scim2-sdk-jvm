# ADR-0006: Jackson as Default Serializer

## Status

Accepted

## Context

SCIM is a JSON-based protocol (RFC 7643/7644). The SDK needs to serialize and deserialize SCIM resources to and from JSON. We need a serialization strategy that is powerful enough to handle SCIM's schema-aware, extensible resource model while remaining pluggable for consumers who prefer alternative libraries.

## Decision

We will use Jackson as the default JSON serialization implementation, provided via a `ScimModule` that registers custom serializers/deserializers for SCIM types. Jackson dependencies in `scim2-sdk-core` are marked as `<optional>true</optional>`.

Serialization is abstracted behind a `ScimSerializer` interface (SPI). The Jackson-based implementation is the default, discovered via service loading. Consumers can provide alternative implementations.

## Consequences

### Positive

- Jackson is the industry standard for JSON processing on the JVM
- Rich feature set: custom serializers, mix-ins, streaming API
- `jackson-module-kotlin` provides excellent Kotlin support
- Optional dependency means core can be used without Jackson if a custom `ScimSerializer` is provided

### Negative

- Jackson is a large dependency (though most JVM projects already include it)
- Custom serializers require knowledge of Jackson internals
- Must maintain compatibility across Jackson versions
