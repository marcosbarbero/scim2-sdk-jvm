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

import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest;
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource;
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse;
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest;
import com.marcosbarbero.scim2.server.port.ResourceHandler;
import com.marcosbarbero.scim2.server.port.ResourceRepository;
import com.marcosbarbero.scim2.server.port.ScimRequestContext;

/**
 * Simple ResourceHandler that delegates to any ResourceRepository.
 * Works from Java without Kotlin type inference issues.
 */
public class SimpleResourceHandler<T extends ScimResource> implements ResourceHandler<T> {

    private final Class<T> type;
    private final String endpointPath;
    private final ResourceRepository<T> repository;

    public SimpleResourceHandler(Class<T> type, String endpoint, ResourceRepository<T> repository) {
        this.type = type;
        this.endpointPath = endpoint;
        this.repository = repository;
    }

    @Override
    public Class<T> getResourceType() {
        return type;
    }

    @Override
    public String getEndpoint() {
        return endpointPath;
    }

    @Override
    public T get(String id, ScimRequestContext context) {
        var resource = repository.findById(id);
        if (resource == null) {
            throw new com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException(type.getSimpleName() + " not found: " + id);
        }
        return resource;
    }

    @Override
    public T create(T resource, ScimRequestContext context) {
        return repository.create(resource);
    }

    @Override
    public T replace(String id, T resource, String version, ScimRequestContext context) {
        return repository.replace(id, resource, version);
    }

    @Override
    public T patch(String id, PatchRequest request, String version, ScimRequestContext context) {
        throw new UnsupportedOperationException("PATCH not supported in plain sample");
    }

    @Override
    public void delete(String id, String version, ScimRequestContext context) {
        repository.delete(id, version);
    }

    @Override
    public ListResponse<T> search(SearchRequest request, ScimRequestContext context) {
        return repository.search(request);
    }
}
