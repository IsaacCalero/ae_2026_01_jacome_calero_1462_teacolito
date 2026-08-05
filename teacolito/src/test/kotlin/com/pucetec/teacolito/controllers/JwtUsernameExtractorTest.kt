package com.pucetec.teacolito.controllers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class JwtUsernameExtractorTest {

    private fun jwt(claims: Map<String, Any>): Jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("fallback-sub")
            .claims { it.putAll(claims) }
            .build()

    @Test
    fun `extractUsername prefers the username claim when present`() {
        val token = jwt(mapOf("username" to "alice", "cognito:username" to "alice-cognito"))

        assertEquals("alice", token.extractUsername())
    }

    @Test
    fun `extractUsername falls back to cognito username when username is absent`() {
        val token = jwt(mapOf("cognito:username" to "alice-cognito"))

        assertEquals("alice-cognito", token.extractUsername())
    }

    @Test
    fun `extractUsername falls back to sub when no username claim exists`() {
        val token = jwt(emptyMap())

        assertEquals("fallback-sub", token.extractUsername())
    }
}
