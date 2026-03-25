package com.marcosbarbero.scim2.core.serialization.jackson

import com.fasterxml.jackson.databind.module.SimpleModule

class ScimModule : SimpleModule() {

    init {
        // SCIM-specific serializers/deserializers can be registered here
        // as the domain model evolves (e.g., custom handling for patches, filters, etc.)
    }
}
