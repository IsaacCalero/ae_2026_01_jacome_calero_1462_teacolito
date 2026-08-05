package com.pucetec.users.logging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

private const val MDC_SUB_KEY = "sub"
private const val ANONYMOUS = "anonimo"

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
    private val objectMapper = ObjectMapper()

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        MDC.put(MDC_SUB_KEY, extractSub(request))
        try {
            log.info("event=http.request | msg=${request.method} ${request.requestURI}")
            filterChain.doFilter(request, response)
        } finally {
            log.info("event=http.response | msg=${response.status} ${request.method} ${request.requestURI}")
            MDC.remove(MDC_SUB_KEY)
        }
    }

    private fun extractSub(request: HttpServletRequest): String {
        val header = request.getHeader("Authorization") ?: return ANONYMOUS
        if (!header.startsWith("Bearer ")) return ANONYMOUS

        val parts = header.removePrefix("Bearer ").split(".")
        if (parts.size < 2) return ANONYMOUS

        return try {
            val payload = String(Base64.getUrlDecoder().decode(parts[1].padToBase64Length()))
            objectMapper.readTree(payload).get("sub")?.asText() ?: ANONYMOUS
        } catch (ex: Exception) {
            ANONYMOUS
        }
    }

    private fun String.padToBase64Length(): String {
        val remainder = length % 4
        return if (remainder == 0) this else this + "=".repeat(4 - remainder)
    }
}
