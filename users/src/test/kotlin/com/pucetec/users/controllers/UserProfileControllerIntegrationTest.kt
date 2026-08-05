package com.pucetec.users.controllers

import com.pucetec.users.IntegrationTestBase
import com.pucetec.users.dto.CreateUserProfileRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserProfileControllerIntegrationTest : IntegrationTestBase() {

    private fun asUser(sub: String) = jwt().jwt { it.subject(sub) }

    @Test
    fun `createProfile without a token returns 401`() {
        mockMvc.perform(
            post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("alice")))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `createProfile with a blank display name returns 400`() {
        mockMvc.perform(
            post("/")
                .with(asUser("sub-blank"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("   ")))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `createProfile creates the profile and returns 201`() {
        mockMvc.perform(
            post("/")
                .with(asUser("sub-create"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("alice-create")))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.username").value("sub-create"))
            .andExpect(jsonPath("$.displayName").value("alice-create"))
    }

    @Test
    fun `createProfile for a sub that already has a profile returns 409`() {
        mockMvc.perform(
            post("/")
                .with(asUser("sub-dup-owner"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("first-name")))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/")
                .with(asUser("sub-dup-owner"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("second-name")))
        ).andExpect(status().isConflict)
    }

    @Test
    fun `createProfile with a display name already taken by someone else returns 409`() {
        mockMvc.perform(
            post("/")
                .with(asUser("sub-original"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("taken-name")))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/")
                .with(asUser("sub-someone-else"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("taken-name")))
        ).andExpect(status().isConflict)
    }

    @Test
    fun `getProfile for an unknown sub returns 404`() {
        mockMvc.perform(get("/does-not-exist").with(asUser("someone")))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getProfile resolves the display name for an existing sub`() {
        mockMvc.perform(
            post("/")
                .with(asUser("sub-lookup"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateUserProfileRequest("lookup-name")))
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/sub-lookup").with(asUser("anyone")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("sub-lookup"))
            .andExpect(jsonPath("$.displayName").value("lookup-name"))
    }

    @Test
    fun `getProfile without a token returns 401`() {
        mockMvc.perform(get("/sub-lookup"))
            .andExpect(status().isUnauthorized)
    }
}
