package com.pucetec.teacolito.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * Replaces the real Cognito-backed JwtDecoder in tests. Integration tests never let this
 * decoder actually run a token through it — they inject an already-authenticated JWT via
 * SecurityMockMvcRequestPostProcessors.jwt(), which bypasses decoding entirely. This bean
 * only needs to exist so the application context can start without calling out to Cognito.
 */
@TestConfiguration
class TestJwtDecoderConfig {

    @Bean
    @Primary
    fun jwtDecoder(): JwtDecoder = JwtDecoder {
        throw UnsupportedOperationException("JWT decoding is bypassed in tests via SecurityMockMvcRequestPostProcessors.jwt()")
    }
}
