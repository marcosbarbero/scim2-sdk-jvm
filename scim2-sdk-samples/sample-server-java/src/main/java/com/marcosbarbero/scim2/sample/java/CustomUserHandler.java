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
package com.marcosbarbero.scim2.sample.java;

import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException;
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest;
import com.marcosbarbero.scim2.core.domain.model.resource.User;
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse;
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest;
import com.marcosbarbero.scim2.server.port.ResourceHandler;
import com.marcosbarbero.scim2.server.port.ScimRequestContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sample {@link ResourceHandler} for {@link User} demonstrating basic in-memory CRUD.
 * Activate by setting {@code sample.custom-handler=true} in application properties.
 */
@Component
@ConditionalOnProperty(name = "sample.custom-handler", havingValue = "true")
public class CustomUserHandler implements ResourceHandler<User> {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    @Override
    public Class<User> getResourceType() {
        return User.class;
    }

    @Override
    public String getEndpoint() {
        return "/Users";
    }

    @Override
    public User get(String id, ScimRequestContext context) {
        User user = store.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        return user;
    }

    @Override
    public User create(User resource, ScimRequestContext context) {
        String id = UUID.randomUUID().toString();
        User created = resource.copy(
                id,
                resource.getExternalId(),
                resource.getMeta(),
                resource.getUserName(),
                resource.getName(),
                resource.getDisplayName(),
                resource.getNickName(),
                resource.getProfileUrl(),
                resource.getTitle(),
                resource.getUserType(),
                resource.getPreferredLanguage(),
                resource.getLocale(),
                resource.getTimezone(),
                resource.getActive(),
                resource.getPassword(),
                resource.getEmails(),
                resource.getPhoneNumbers(),
                resource.getIms(),
                resource.getPhotos(),
                resource.getAddresses(),
                resource.getGroups(),
                resource.getEntitlements(),
                resource.getRoles(),
                resource.getX509Certificates()
        );
        store.put(id, created);
        return created;
    }

    @Override
    public User replace(String id, User resource, String version, ScimRequestContext context) {
        if (!store.containsKey(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        User replaced = resource.copy(
                id,
                resource.getExternalId(),
                resource.getMeta(),
                resource.getUserName(),
                resource.getName(),
                resource.getDisplayName(),
                resource.getNickName(),
                resource.getProfileUrl(),
                resource.getTitle(),
                resource.getUserType(),
                resource.getPreferredLanguage(),
                resource.getLocale(),
                resource.getTimezone(),
                resource.getActive(),
                resource.getPassword(),
                resource.getEmails(),
                resource.getPhoneNumbers(),
                resource.getIms(),
                resource.getPhotos(),
                resource.getAddresses(),
                resource.getGroups(),
                resource.getEntitlements(),
                resource.getRoles(),
                resource.getX509Certificates()
        );
        store.put(id, replaced);
        return replaced;
    }

    @Override
    public User patch(String id, PatchRequest request, String version, ScimRequestContext context) {
        User existing = get(id, context);
        // Simplified: return existing without applying patch operations
        return existing;
    }

    @Override
    public void delete(String id, String version, ScimRequestContext context) {
        if (store.remove(id) == null) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
    }

    @Override
    public ListResponse<User> search(SearchRequest request, ScimRequestContext context) {
        List<User> users = new ArrayList<>(store.values());
        return new ListResponse<>(
                Collections.singletonList("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                users.size(),
                users.size(),
                1,
                users
        );
    }
}
