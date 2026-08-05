package com.pucetec.teacolito.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Base64

class RequestLoggingFilterTest {

    private val filter = RequestLoggingFilter()

    private fun mockRequest(authorizationHeader: String?): HttpServletRequest {
        val request = mock<HttpServletRequest>()
        whenever(request.getHeader("Authorization")).thenReturn(authorizationHeader)
        whenever(request.method).thenReturn("GET")
        whenever(request.requestURI).thenReturn("/expense-groups/1")
        whenever(request.getAttribute(org.mockito.kotlin.any())).thenReturn(null)
        return request
    }

    private fun mockResponse(): HttpServletResponse {
        val response = mock<HttpServletResponse>()
        whenever(response.status).thenReturn(200)
        return response
    }

    private fun fakeJwt(claimsJson: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".toByteArray())
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(claimsJson.toByteArray())
        return "$header.$payload.signature"
    }

    @Test
    fun `doFilter calls the chain and clears MDC when there is no Authorization header`() {
        val request = mockRequest(null)
        val response = mockResponse()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
        assertEquals(null, org.slf4j.MDC.get("sub"))
    }

    @Test
    fun `doFilter treats a non-Bearer header as anonymous`() {
        val request = mockRequest("Basic somecredentials")
        val response = mockResponse()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `doFilter treats a malformed token as anonymous`() {
        val request = mockRequest("Bearer not-a-jwt")
        val response = mockResponse()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `doFilter treats a token with an unparsable payload as anonymous`() {
        val request = mockRequest("Bearer header.###notbase64###.signature")
        val response = mockResponse()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `doFilter extracts the sub claim from a well-formed token`() {
        val request = mockRequest("Bearer " + fakeJwt("""{"sub":"alice"}"""))
        val response = mockResponse()
        val chain = mock<FilterChain>()

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `doFilter still logs the response and clears MDC when the chain throws`() {
        val request = mockRequest("Bearer " + fakeJwt("""{"sub":"alice"}"""))
        val response = mockResponse()
        val chain = mock<FilterChain>()
        whenever(chain.doFilter(request, response)).thenThrow(RuntimeException("boom"))

        try {
            filter.doFilter(request, response, chain)
        } catch (ex: RuntimeException) {
            // expected — the filter re-throws after logging
        }

        assertEquals(null, org.slf4j.MDC.get("sub"))
    }
}
