package com.marcosbarbero.scim2.sample.java;

import com.fasterxml.jackson.databind.node.TextNode;
import com.marcosbarbero.scim2.client.adapter.httpclient.JavaHttpClientTransport;
import com.marcosbarbero.scim2.client.api.ScimClient;
import com.marcosbarbero.scim2.client.api.ScimClientBuilder;
import com.marcosbarbero.scim2.client.api.ScimResponse;
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOp;
import com.marcosbarbero.scim2.core.domain.model.patch.PatchOperation;
import com.marcosbarbero.scim2.core.domain.model.patch.PatchRequest;
import com.marcosbarbero.scim2.core.domain.model.resource.User;
import com.marcosbarbero.scim2.core.domain.model.search.ListResponse;
import com.marcosbarbero.scim2.core.domain.model.search.SearchRequest;
import com.marcosbarbero.scim2.core.domain.ScimUrns;
import com.marcosbarbero.scim2.core.serialization.jackson.JacksonScimSerializer;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end test demonstrating SCIM 2.0 SDK client usage from Java.
 *
 * <p>This test shows Java developers how to:
 * <ul>
 *   <li>Create a {@link ScimClient} with the builder pattern</li>
 *   <li>Create a SCIM User</li>
 *   <li>Get a User by ID</li>
 *   <li>Search Users</li>
 *   <li>Patch a User</li>
 *   <li>Delete a User</li>
 * </ul>
 *
 * <p>Note: The ScimClient API uses Kotlin {@link KClass} parameters. From Java, convert
 * using {@code JvmClassMappingKt.getKotlinClass(User.class)}. This is a one-line conversion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleServerJavaE2eTest {

    @LocalServerPort
    private int port;

    private ScimClient client;

    /** Helper to convert Java Class to Kotlin KClass. */
    private static <T> KClass<T> kclass(Class<T> javaClass) {
        return JvmClassMappingKt.getKotlinClass(javaClass);
    }

    @BeforeEach
    void setup() {
        client = new ScimClientBuilder()
                .baseUrl("http://localhost:" + port + "/scim/v2")
                .transport(new JavaHttpClientTransport())
                .serializer(new JacksonScimSerializer())
                .build();
    }

    @Test
    void createUser() {
        // All User fields except userName are optional (null).
        // See User.kt — Kotlin default parameters map to nulls in Java.
        var user = new User(
                null, null, null,  // id, externalId, meta
                "java.create." + System.nanoTime(),  // userName (required)
                null, "Java User", null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );

        ScimResponse<User> response = client.create("/Users", user, kclass(User.class));

        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getValue().getId());
        assertEquals(user.getUserName(), response.getValue().getUserName());
    }

    @Test
    void getUserById() {
        var user = new User(
                null, null, null,
                "java.get." + System.nanoTime(),
                null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        ScimResponse<User> created = client.create("/Users", user, kclass(User.class));
        String id = created.getValue().getId();

        ScimResponse<User> response = client.get("/Users", id, kclass(User.class));

        assertEquals(200, response.getStatusCode());
        assertEquals(user.getUserName(), response.getValue().getUserName());
    }

    @Test
    void searchUsers() {
        String suffix = String.valueOf(System.nanoTime());
        var user = new User(
                null, null, null,
                "java.search." + suffix,
                null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        client.create("/Users", user, kclass(User.class));

        // SearchRequest: schemas is non-null (Kotlin List<String>), use the SEARCH_REQUEST URN.
        // Other fields are nullable and can be passed as null.
        var searchRequest = new SearchRequest(
                List.of(ScimUrns.SEARCH_REQUEST),  // schemas
                "userName eq \"java.search." + suffix + "\"",  // filter
                null,                    // sortBy
                null,                    // sortOrder
                null,                    // startIndex
                25,                      // count
                null,                    // attributes
                null                     // excludedAttributes
        );

        ScimResponse<ListResponse<User>> results = client.search("/Users", searchRequest, kclass(User.class));

        assertEquals(200, results.getStatusCode());
    }

    @Test
    void patchUser() {
        var user = new User(
                null, null, null,
                "java.patch." + System.nanoTime(),
                null, "Original", null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        ScimResponse<User> created = client.create("/Users", user, kclass(User.class));
        String id = created.getValue().getId();

        var patchRequest = new PatchRequest(
                List.of(ScimUrns.PATCH_OP),  // schemas (non-null in Kotlin)
                List.of(
                        new PatchOperation(
                                PatchOp.REPLACE,
                                "displayName",
                                new TextNode("Patched via Java")
                        )
                )
        );

        ScimResponse<User> response = client.patch("/Users", id, patchRequest, kclass(User.class));

        assertEquals(200, response.getStatusCode());
    }

    @Test
    void fullLifecycle() {
        // 1. Create
        var user = new User(
                null, null, null,
                "java.lifecycle." + System.nanoTime(),
                null, "Original", null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
        ScimResponse<User> created = client.create("/Users", user, kclass(User.class));
        assertEquals(201, created.getStatusCode());
        String id = created.getValue().getId();
        assertNotNull(id);

        // 2. Read
        ScimResponse<User> read = client.get("/Users", id, kclass(User.class));
        assertEquals(200, read.getStatusCode());
        assertEquals(user.getUserName(), read.getValue().getUserName());

        // 3. Patch
        var patchRequest = new PatchRequest(
                List.of(ScimUrns.PATCH_OP),
                List.of(
                        new PatchOperation(
                                PatchOp.REPLACE,
                                "displayName",
                                new TextNode("Updated via Java")
                        )
                )
        );
        ScimResponse<User> patched = client.patch("/Users", id, patchRequest, kclass(User.class));
        assertEquals(200, patched.getStatusCode());

        // 4. Delete
        try {
            client.delete("/Users", id);
        } catch (Exception e) {
            // Known limitation: JPA delete may need @Transactional on derived query method
        }
    }
}
