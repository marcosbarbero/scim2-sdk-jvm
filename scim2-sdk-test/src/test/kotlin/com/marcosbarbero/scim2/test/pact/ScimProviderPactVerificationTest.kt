package com.marcosbarbero.scim2.test.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Pact Provider verification test.
 *
 * Verifies that our SCIM server implementation satisfies the contracts
 * defined by the ScimClient consumer tests.
 *
 * The pact files are loaded from the client module's target/pacts directory.
 *
 * Note: This test requires an actual HTTP server to be started.
 * Wire up the JDK HttpServer with ScimEndpointDispatcher + InMemoryResourceHandler
 * (similar to sample-server-plain) in a @BeforeAll method to set the serverPort.
 */
@Disabled("Provider verification requires in-memory SCIM server wiring -- see TODO in companion object")
@Provider("ScimServiceProvider")
@PactFolder("../../scim2-sdk-client/target/pacts")
class ScimProviderPactVerificationTest {

    companion object {
        // TODO: Start an in-memory SCIM server in @BeforeAll and assign the port here.
        // Use the JDK HttpServer with ScimEndpointDispatcher + InMemoryResourceHandler
        // (similar to sample-server-plain).
        var serverPort: Int = 0
    }

    @BeforeEach
    fun setUp(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", serverPort)
    }

    @State("no users exist")
    fun noUsersExist() {
        // Clear the in-memory store
    }

    @State("a user with id 123 exists")
    fun userExists() {
        // Create a user with id 123 in the in-memory store
    }

    @State("no user with id 999 exists")
    fun userDoesNotExist() {
        // Ensure no user with id 999
    }

    @State("users exist")
    fun usersExist() {
        // Create some sample users
    }

    @State("server is running")
    fun serverRunning() {
        // No-op -- server is already running
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext) {
        context.verifyInteraction()
    }
}
