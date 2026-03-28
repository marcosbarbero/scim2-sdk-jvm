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

import com.marcosbarbero.scim2.core.domain.model.resource.Group;
import com.marcosbarbero.scim2.core.domain.model.resource.User;
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry;
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer;
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService;
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher;
import com.marcosbarbero.scim2.server.config.ScimServerConfig;
import com.marcosbarbero.scim2.server.engine.ETagEngine;
import com.marcosbarbero.scim2.server.http.HttpMethod;
import com.marcosbarbero.scim2.server.http.ScimHttpRequest;
import com.marcosbarbero.scim2.server.port.ResourceHandler;
import com.marcosbarbero.scim2.server.provisioning.ScimOutboundEventPublisher;
import com.marcosbarbero.scim2.server.provisioning.ScimOutboundTarget;
import com.marcosbarbero.scim2.client.adapter.httpclient.HttpClientTransport;
import com.marcosbarbero.scim2.client.api.ScimClient;
import com.marcosbarbero.scim2.client.api.ScimClientBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production-like SCIM 2.0 server using only the JDK HTTP server — no Spring Boot.
 * Demonstrates that scim2-sdk-jvm works without any framework, with PostgreSQL via plain JDBC.
 */
public class PlainScimServer {

    public static void main(String[] args) throws Exception {
        var port = Integer.parseInt(env("PORT", "8080"));
        var baseUrl = env("SCIM_BASE_URL", "http://localhost:" + port);
        var config = new ScimServerConfig(
                "/scim/v2", true, 1000, 1_048_576L, true, 200,
                false, true, false, true, 100, 1000, baseUrl
        );
        var serializer = new JacksonScimSerializer();

        // PostgreSQL data source
        var dbUrl = env("DATABASE_URL", "jdbc:postgresql://localhost:5432/scimdb");
        var ds = new PGSimpleDataSource();
        ds.setUrl(dbUrl);
        ds.setUser(env("DATABASE_USER", "scim"));
        ds.setPassword(env("DATABASE_PASSWORD", "scim"));

        initSchema(ds);

        // Create repositories and handlers
        var userRepo = new JdbcResourceRepository<>(ds, serializer, User.class, "User");
        var groupRepo = new JdbcResourceRepository<>(ds, serializer, Group.class, "Group");
        ResourceHandler<User> userHandler = new SimpleResourceHandler<>(User.class, "/Users", userRepo);
        ResourceHandler<Group> groupHandler = new SimpleResourceHandler<>(Group.class, "/Groups", groupRepo);
        List<ResourceHandler<?>> handlers = List.of(userHandler, groupHandler);

        // Schema registry
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.register(kotlin.jvm.JvmClassMappingKt.getKotlinClass(User.class));
        schemaRegistry.register(kotlin.jvm.JvmClassMappingKt.getKotlinClass(Group.class));

        // Outbound provisioning — push changes to a target SCIM server (if configured)
        var targetUrl = System.getenv("SCIM_TARGET_URL");
        com.marcosbarbero.scim2.core.event.ScimEventPublisher eventPublisher = com.marcosbarbero.scim2.core.event.NoOpEventPublisher.INSTANCE;
        if (targetUrl != null && !targetUrl.isBlank()) {
            System.out.println("Outbound provisioning enabled: " + targetUrl);
            var scimClient = new ScimClientBuilder()
                    .baseUrl(targetUrl)
                    .transport(new HttpClientTransport())
                    .serializer(serializer)
                    .build();
            @SuppressWarnings("unchecked")
            ScimOutboundTarget target = new ScimClientOutboundTarget(scimClient);
            eventPublisher = new ScimOutboundEventPublisher(target, handlers);
        }

        // Discovery + dispatcher
        var discoveryService = new DiscoveryService(handlers, schemaRegistry, config);
        var dispatcher = new ScimEndpointDispatcher(
                handlers, null, null, discoveryService, config, serializer,
                new ETagEngine(), List.of(), null, null,
                eventPublisher,
                com.marcosbarbero.scim2.core.observability.NoOpScimMetrics.INSTANCE,
                com.marcosbarbero.scim2.core.observability.NoOpScimTracer.INSTANCE
        );

        // Start JDK HTTP server with virtual threads
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/scim/v2", exchange -> {
            // CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            var scimRequest = toScimHttpRequest(exchange);
            var scimResponse = dispatcher.dispatch(scimRequest);

            scimResponse.getHeaders().forEach((key, value) ->
                    exchange.getResponseHeaders().add(key, value));

            var body = scimResponse.getBody();
            if (body == null || body.length == 0) {
                exchange.sendResponseHeaders(scimResponse.getStatus(), -1);
            } else {
                exchange.sendResponseHeaders(scimResponse.getStatus(), body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("Plain SCIM server started on http://localhost:" + port + "/scim/v2");
        System.out.println("Database: " + dbUrl);
        System.out.println("Try: curl http://localhost:" + port + "/scim/v2/ServiceProviderConfig");
    }

    private static void initSchema(PGSimpleDataSource ds) throws Exception {
        var sql = """
                CREATE TABLE IF NOT EXISTS scim_resources (
                    id              VARCHAR(255) NOT NULL PRIMARY KEY,
                    resource_type   VARCHAR(100) NOT NULL,
                    external_id     VARCHAR(255),
                    display_name    VARCHAR(500),
                    resource_json   TEXT NOT NULL,
                    version         BIGINT NOT NULL DEFAULT 1,
                    created         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_modified   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_scim_resources_type ON scim_resources (resource_type);
                CREATE INDEX IF NOT EXISTS idx_scim_resources_external_id ON scim_resources (resource_type, external_id);
                """;
        try (var conn = ds.getConnection(); var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        System.out.println("Database schema initialized");
    }

    private static ScimHttpRequest toScimHttpRequest(HttpExchange exchange) throws IOException {
        var method = HttpMethod.valueOf(exchange.getRequestMethod().toUpperCase());
        var path = exchange.getRequestURI().getPath();
        Map<String, List<String>> headers = new HashMap<>(exchange.getRequestHeaders());
        var queryParams = parseQueryParameters(exchange.getRequestURI());
        byte[] body = exchange.getRequestBody().readAllBytes();
        if (body.length == 0) body = null;
        return new ScimHttpRequest(method, path, headers, queryParams, body);
    }

    private static Map<String, List<String>> parseQueryParameters(URI uri) {
        var query = uri.getRawQuery();
        if (query == null) return Map.of();
        var result = new HashMap<String, List<String>>();
        for (var param : query.split("&")) {
            var parts = param.split("=", 2);
            var key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            var value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return result;
    }

    private static String env(String key, String defaultValue) {
        var value = System.getenv(key);
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
