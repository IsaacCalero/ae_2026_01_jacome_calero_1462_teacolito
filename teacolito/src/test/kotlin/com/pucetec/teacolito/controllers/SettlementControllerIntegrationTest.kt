package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.IntegrationTestBase
import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.JoinGroupRequest
import com.pucetec.teacolito.dto.SettlementRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class SettlementControllerIntegrationTest : IntegrationTestBase() {

    private fun asUser(username: String) = jwt().jwt { it.subject(username) }

    private fun createGroupWithMember(name: String, creator: String, member: String): Long {
        val response = mockMvc.perform(
            post("/expense-groups")
                .with(asUser(creator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ExpenseGroupRequest(name)))
        ).andReturn()

        val group = objectMapper.readValue(response.response.contentAsString, Map::class.java)
        val groupId = (group["id"] as Number).toLong()
        val inviteCode = group["inviteCode"] as String

        mockMvc.perform(
            post("/groups/join")
                .with(asUser(member))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(JoinGroupRequest(inviteCode)))
        )

        return groupId
    }

    @Test
    fun `createSettlement without a token returns 401`() {
        mockMvc.perform(
            post("/settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SettlementRequest(1L, "alice", BigDecimal("10.00"))))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `createSettlement registers the payment between members`() {
        val groupId = createGroupWithMember("Settle up", "alice", "bob")

        mockMvc.perform(
            post("/settlements")
                .with(asUser("bob"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SettlementRequest(groupId, "alice", BigDecimal("15.00"))))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fromUsername").value("bob"))
            .andExpect(jsonPath("$.toUsername").value("alice"))
    }

    @Test
    fun `getSettlementsByGroup by a non-member returns 403`() {
        val groupId = createGroupWithMember("Members only", "alice", "bob")

        mockMvc.perform(get("/expense-groups/$groupId/settlements").with(asUser("mallory")))
            .andExpect(status().isForbidden)
    }
}
