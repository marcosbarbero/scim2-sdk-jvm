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

import com.marcosbarbero.scim2.core.domain.model.resource.Group;
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

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final ResourceHandler<Group> groupHandler;

    public GroupController(ResourceHandler<Group> groupHandler) {
        this.groupHandler = groupHandler;
    }

    @GetMapping
    public ResponseEntity<ListResponse<Group>> listGroups(
            @RequestParam(defaultValue = "1") int startIndex,
            @RequestParam(defaultValue = "20") int count,
            @RequestParam(required = false) String filter,
            @AuthenticationPrincipal Jwt jwt) {

        var filterValue = (filter != null && !filter.isBlank()) ? filter : null;
        var search = new SearchRequest(List.of(), filterValue, null, null, startIndex, count, null, null);

        var ctx = buildContext(jwt);
        var result = groupHandler.search(search, ctx);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroup(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        var group = groupHandler.get(id, ctx);
        return ResponseEntity.ok(group);
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(
            @RequestBody Group group,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        var created = groupHandler.create(group, ctx);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Group> replaceGroup(
            @PathVariable String id,
            @RequestBody Group group,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        var replaced = groupHandler.replace(id, group, null, ctx);
        return ResponseEntity.ok(replaced);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        var ctx = buildContext(jwt);
        groupHandler.delete(id, null, ctx);
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
