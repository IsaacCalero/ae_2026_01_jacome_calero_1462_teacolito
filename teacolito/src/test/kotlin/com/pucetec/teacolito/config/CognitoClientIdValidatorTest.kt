package com.pucetec.teacolito.config

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class CognitoClientIdValidatorTest {

    private val validator = CognitoClientIdValidator("expected-client-id")

    private fun jwtWithClientId(clientId: String?): Jwt {
        val builder = Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("alice")

        if (clientId != null) {
            builder.claim("client_id", clientId)
        } else {
            builder.claim("placeholder", "value")
        }

        return builder.build()
    }

    @Test
    fun `validate succeeds when client_id matches the configured App Client`() {
        val result = validator.validate(jwtWithClientId("expected-client-id"))

        assertFalse(result.hasErrors())
    }

    @Test
    fun `validate fails when client_id does not match`() {
        val result = validator.validate(jwtWithClientId("some-other-client"))

        assertTrue(result.hasErrors())
    }

    @Test
    fun `validate fails when client_id claim is missing`() {
        val result = validator.validate(jwtWithClientId(null))

        assertTrue(result.hasErrors())
    }
}
