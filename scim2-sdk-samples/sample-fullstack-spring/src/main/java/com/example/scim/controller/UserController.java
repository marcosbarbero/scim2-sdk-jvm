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
package com.example.scim.controller;

import com.marcosbarbero.scim2.core.domain.model.resource.User;
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse;
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest;
import com.marcosbarbero.scim2.server.port.ResourceHandler;
import com.marcosbarbero.scim2.server.port.ScimRequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for the React frontend.
 * Delegates to the SCIM ResourceHandler so all mutations go through
 * the same path as SCIM protocol requests.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ResourceHandler<User> userHandler;

    public UserController(ResourceHandler<User> userHandler) {
        this.userHandler = userHandler;
    }

    @GetMapping
    public ResponseEntity<ListResponse<User>> listUsers(
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) String filter,
            @AuthenticationPrincipal Jwt jwt) {

        var filterValue = (filter != null && !filter.isBlank()) ? filter : null;
        var search = new SearchRequest(List.of(), filterValue, null, null, startIndex, count, null, null);

        var ctx = buildContext(jwt);
        var result = userHandler.search(search, ctx);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        var user = userHandler.get(id, ctx);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody User user,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        var created = userHandler.create(user, ctx);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> replaceUser(
            @PathVariable String id,
            @RequestBody User user,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        var replaced = userHandler.replace(id, user, null, ctx);
        return ResponseEntity.ok(replaced);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        userHandler.delete(id, null, ctx);
        return ResponseEntity.noContent().build();
    }

    private ScimRequestContext buildContext(Jwt jwt) {
        var roles = extractRoles(jwt);
        return new ScimRequestContext(jwt.getSubject(), roles, Map.of(), List.of(), List.of(), UUID.randomUUID().toString());
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Jwt jwt) {
        var realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess == null) return Set.of();
        var roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return Set.of();
        return new HashSet<>(roles);
    }
}
