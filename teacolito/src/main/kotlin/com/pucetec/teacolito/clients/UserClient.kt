package com.pucetec.teacolito.clients

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

data class UserProfileResponse(
    val username: String,
    val displayName: String
)

@Component
class UserClient(
    @Value("\${users.service.url}") private val usersServiceUrl: String
) {
    private val log = LoggerFactory.getLogger(UserClient::class.java)
    private val restClient = RestClient.create()

    fun resolveDisplayName(username: String, bearerToken: String): String {
        return try {
            restClient.get()
                .uri("$usersServiceUrl/users/{username}", username)
                .header("Authorization", "Bearer $bearerToken")
                .retrieve()
                .body(UserProfileResponse::class.java)
                ?.displayName
                ?: username
        } catch (ex: RestClientException) {
            log.warn("event=users.resolve.failed msg=Could not resolve display name for user / username={} error={}", username, ex.message?.replace("|", "/"))
            username
        }
    }
}
