package com.pucetec.teacolito.config

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Cognito Access Tokens carry the App Client id in the "client_id" claim,
 * not the standard "aud" claim used by ID tokens — so the default audience
 * validator doesn't apply here and this one checks "client_id" instead.
 */
class CognitoClientIdValidator(private val expectedClientId: String) : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val clientId = token.getClaimAsString("client_id")
        return if (clientId == expectedClientId) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "The client_id claim does not match the configured App Client", null)
            )
        }
    }
}
