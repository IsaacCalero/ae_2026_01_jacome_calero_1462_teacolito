package com.pucetec.users

import com.fasterxml.jackson.databind.ObjectMapper
import com.pucetec.users.config.TestJwtDecoderConfig
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer

// Deliberately not using @Testcontainers/@Container here: that combination stops the
// container after each test CLASS's @AfterAll, but this container is shared (via this
// inherited companion object) across every integration test class in the suite. Instead
// this follows the Testcontainers "singleton container" pattern — started once, manually,
// and left for the Ryuk reaper to clean up when the JVM exits.
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig::class)
abstract class IntegrationTestBase {

    @Autowired
    lateinit var mockMvc: MockMvc

    // Not an injected Spring bean on purpose — this Spring Boot 4 setup doesn't expose a
    // default ObjectMapper bean in this context, and these tests only need plain
    // (de)serialization of request/response DTOs.
    val objectMapper = ObjectMapper()

    companion object {
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine").apply { start() }
    }
}
