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

import com.marcosbarbero.scim2.core.domain.model.common.Meta;
import com.marcosbarbero.scim2.core.domain.model.error.ResourceNotFoundException;
import com.marcosbarbero.scim2.core.domain.model.resource.ScimResource;
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse;
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest;
import com.marcosbarbero.scim2.core.domain.vo.ETag;
import com.marcosbarbero.scim2.core.serialization.spi.ScimSerializer;
import com.marcosbarbero.scim2.server.port.ResourceRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plain JDBC implementation of {@link ResourceRepository}.
 * Uses the same scim_resources table schema as the SDK's Flyway migration.
 * No Spring, no JPA — just JDBC.
 */
public class JdbcResourceRepository<T extends ScimResource> implements ResourceRepository<T> {

    private final DataSource dataSource;
    private final ScimSerializer serializer;
    private final Class<T> resourceType;
    private final String resourceTypeName;

    public JdbcResourceRepository(DataSource dataSource, ScimSerializer serializer, Class<T> resourceType, String resourceTypeName) {
        this.dataSource = dataSource;
        this.serializer = serializer;
        this.resourceType = resourceType;
        this.resourceTypeName = resourceTypeName;
    }

    @Override
    public T create(T resource) {
        // Idempotency: check externalId
        if (resource.getExternalId() != null) {
            var existing = findByExternalId(resource.getExternalId());
            if (existing != null) {
                return replace(existing.getId(), resource, null);
            }
        }

        var id = resource.getId() != null ? resource.getId() : UUID.randomUUID().toString();
        var now = Instant.now();
        var meta = new Meta(resourceTypeName, now, now, null, new ETag("W/\"1\""));
        var withMeta = setIdAndMeta(resource, id, meta);

        var json = serializer.serializeToString(withMeta);
        var sql = "INSERT INTO scim_resources (id, resource_type, external_id, display_name, resource_json, version, created, last_modified) VALUES (?, ?, ?, ?, ?, 1, ?, ?)";

        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, resourceTypeName);
            stmt.setString(3, resource.getExternalId());
            stmt.setString(4, extractDisplayName(withMeta));
            stmt.setString(5, json);
            stmt.setTimestamp(6, Timestamp.from(now));
            stmt.setTimestamp(7, Timestamp.from(now));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create resource", e);
        }
        return withMeta;
    }

    @Override
    public T findById(String id) {
        var sql = "SELECT resource_json FROM scim_resources WHERE id = ? AND resource_type = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, resourceTypeName);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return deserialize(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find resource", e);
        }
    }

    @Override
    public T replace(String id, T resource, String version) {
        var sql = "SELECT version, created FROM scim_resources WHERE id = ? AND resource_type = ?";
        long currentVersion;
        Instant created;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, resourceTypeName);
            var rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new ResourceNotFoundException(resourceTypeName + " not found: " + id);
            }
            currentVersion = rs.getLong("version");
            created = rs.getTimestamp("created").toInstant();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find resource for replace", e);
        }

        var now = Instant.now();
        var newVersion = currentVersion + 1;
        var meta = new Meta(resourceTypeName, created, now, null, new ETag("W/\"" + newVersion + "\""));
        var withMeta = setIdAndMeta(resource, id, meta);
        var json = serializer.serializeToString(withMeta);

        var updateSql = "UPDATE scim_resources SET resource_json = ?, external_id = ?, display_name = ?, version = ?, last_modified = ? WHERE id = ? AND resource_type = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, json);
            stmt.setString(2, resource.getExternalId());
            stmt.setString(3, extractDisplayName(withMeta));
            stmt.setLong(4, newVersion);
            stmt.setTimestamp(5, Timestamp.from(now));
            stmt.setString(6, id);
            stmt.setString(7, resourceTypeName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to replace resource", e);
        }
        return withMeta;
    }

    @Override
    public void delete(String id, String version) {
        var sql = "DELETE FROM scim_resources WHERE id = ? AND resource_type = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, resourceTypeName);
            var deleted = stmt.executeUpdate();
            if (deleted == 0) {
                throw new ResourceNotFoundException(resourceTypeName + " not found: " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete resource", e);
        }
    }

    @Override
    public ListResponse<T> search(SearchRequest request) {
        var startIndex = request.getStartIndex() != null ? request.getStartIndex() : 1;
        var count = request.getCount() != null ? request.getCount() : 100;
        var offset = Math.max(0, startIndex - 1);

        var countSql = "SELECT COUNT(*) FROM scim_resources WHERE resource_type = ?";
        int total;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(countSql)) {
            stmt.setString(1, resourceTypeName);
            var rs = stmt.executeQuery();
            rs.next();
            total = rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count resources", e);
        }

        var sql = "SELECT resource_json FROM scim_resources WHERE resource_type = ? ORDER BY created LIMIT ? OFFSET ?";
        var resources = new ArrayList<T>();
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, resourceTypeName);
            stmt.setInt(2, count);
            stmt.setInt(3, offset);
            var rs = stmt.executeQuery();
            while (rs.next()) {
                resources.add(deserialize(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search resources", e);
        }

        return new ListResponse<>(total, resources.size(), startIndex, resources);
    }

    private T findByExternalId(String externalId) {
        var sql = "SELECT resource_json FROM scim_resources WHERE resource_type = ? AND external_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, resourceTypeName);
            stmt.setString(2, externalId);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return deserialize(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by externalId", e);
        }
    }

    private T deserialize(ResultSet rs) throws SQLException {
        var json = rs.getString("resource_json");
        return serializer.deserializeFromString(json, kotlin.jvm.JvmClassMappingKt.getKotlinClass(resourceType));
    }

    @SuppressWarnings("unchecked")
    private T setIdAndMeta(T resource, String id, Meta meta) {
        // Use Jackson to inject id and meta into the resource
        var json = serializer.serializeToString(resource);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        try {
            var node = (tools.jackson.databind.node.ObjectNode) mapper.readTree(json);
            node.put("id", id);
            node.set("meta", mapper.readTree(serializer.serializeToString(meta)));
            var updatedJson = mapper.writeValueAsString(node);
            return serializer.deserializeFromString(updatedJson, kotlin.jvm.JvmClassMappingKt.getKotlinClass(resourceType));
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id and meta", e);
        }
    }

    private String extractDisplayName(T resource) {
        if (resource instanceof com.marcosbarbero.scim2.core.domain.model.resource.User user) {
            return user.getDisplayName() != null ? user.getDisplayName() : user.getUserName();
        }
        if (resource instanceof com.marcosbarbero.scim2.core.domain.model.resource.Group group) {
            return group.getDisplayName();
        }
        return null;
    }
}
