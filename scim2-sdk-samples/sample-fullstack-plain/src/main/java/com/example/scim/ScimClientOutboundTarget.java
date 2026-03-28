/*
 * Copyright 2026 Marcos Barbero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.scim;

import com.marcosbarbero.scim2.client.api.ScimClient;
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource;
import com.marcosbarbero.scim2.server.provisioning.ScimOutboundTarget;
import kotlin.reflect.KClass;

/**
 * Bridges {@link ScimOutboundTarget} to {@link ScimClient}.
 */
@SuppressWarnings("unchecked")
public class ScimClientOutboundTarget implements ScimOutboundTarget {

    private final ScimClient scimClient;

    public ScimClientOutboundTarget(ScimClient scimClient) {
        this.scimClient = scimClient;
    }

    @Override
    public void create(String endpoint, ScimResource resource) {
        KClass<ScimResource> type = (KClass<ScimResource>) kotlin.jvm.JvmClassMappingKt.getKotlinClass(resource.getClass());
        scimClient.create(endpoint, resource, type);
    }

    @Override
    public void replace(String endpoint, String id, ScimResource resource) {
        KClass<ScimResource> type = (KClass<ScimResource>) kotlin.jvm.JvmClassMappingKt.getKotlinClass(resource.getClass());
        scimClient.replace(endpoint, id, resource, type);
    }

    @Override
    public void delete(String endpoint, String id) {
        scimClient.delete(endpoint, id);
    }
}
