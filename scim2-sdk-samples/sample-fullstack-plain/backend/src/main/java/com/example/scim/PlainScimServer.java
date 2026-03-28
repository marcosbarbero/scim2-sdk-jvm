package com.example.scim;

import com.marcosbarbero.scim2.core.domain.model.resource.Group;
import com.marcosbarbero.scim2.core.domain.model.resource.User;
import com.marcosbarbero.scim2.core.schema.introspector.SchemaRegistry;
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer;
import com.marcosbarbero.scim2.server.adapter.discovery.DiscoveryService;
import com.marcosbarbero.scim2.server.adapter.http.ScimEndpointDispatcher;
import com.marcosbarbero.scim2.server.config.ScimServerConfig;
import com.marcosbarbero.scim2.server.http.HttpMethod;
import com.marcosbarbero.scim2.server.http.ScimHttpRequest;
import com.marcosbarbero.scim2.test.handler.InMemoryResourceHandler;
import com.marcosbarbero.scim2.test.repository.InMemoryResourceRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

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
 * Minimal SCIM 2.0 server using only the JDK HTTP server — no Spring Boot.
 * Demonstrates that scim2-sdk-jvm works without any framework.
 *
 * <p>Run: {@code java -jar backend/target/scim-fullstack-plain-backend-0.0.1-SNAPSHOT.jar}
 */
public class PlainScimServer {

    public static void main(String[] args) throws IOException {
        var port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        var config = new ScimServerConfig("/scim/v2");
        var serializer = new JacksonScimSerializer();

        // In-memory repositories
        var userRepo = new InMemoryResourceRepository<>(User.class, (user, id, meta) -> user.copy(
                user.getSchemas(), id, user.getExternalId(), meta, user.getUserName(), user.getName(),
                user.getDisplayName(), user.getNickName(), user.getProfileUrl(), user.getTitle(),
                user.getUserType(), user.getPreferredLanguage(), user.getLocale(), user.getTimezone(),
                user.getActive(), user.getPassword(), user.getEmails(), user.getPhoneNumbers(),
                user.getIms(), user.getPhotos(), user.getAddresses(), user.getGroups(),
                user.getEntitlements(), user.getRoles(), user.getX509Certificates()
        ));
        var groupRepo = new InMemoryResourceRepository<>(Group.class, (group, id, meta) -> group.copy(
                group.getSchemas(), id, group.getExternalId(), meta, group.getDisplayName(), group.getMembers()
        ));

        var userHandler = new InMemoryResourceHandler<>(User.class, "/Users", userRepo);
        var groupHandler = new InMemoryResourceHandler<>(Group.class, "/Groups", groupRepo);

        // Schema registry
        var schemaRegistry = new SchemaRegistry();
        schemaRegistry.register(kotlin.jvm.JvmClassMappingKt.getKotlinClass(User.class));
        schemaRegistry.register(kotlin.jvm.JvmClassMappingKt.getKotlinClass(Group.class));

        // Discovery + dispatcher
        var handlers = List.of(userHandler, groupHandler);
        var discoveryService = new DiscoveryService(handlers, schemaRegistry, config);
        var dispatcher = new ScimEndpointDispatcher(
                handlers, null, null, discoveryService, config, serializer,
                List.of(), null, null, null, null, null
        );

        // Start JDK HTTP server
        var server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/scim/v2", exchange -> {
            var scimRequest = toScimHttpRequest(exchange);
            var scimResponse = dispatcher.dispatch(scimRequest);

            // CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

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
        server.setExecutor(null);
        server.start();

        System.out.println("Plain SCIM server started on http://localhost:" + port + "/scim/v2");
        System.out.println("Try: curl http://localhost:" + port + "/scim/v2/ServiceProviderConfig");
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
}
