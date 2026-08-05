package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.IntegrationTestBase
import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.ExpenseRequest
import com.pucetec.teacolito.dto.ExpenseShareRequest
import com.pucetec.teacolito.dto.JoinGroupRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class ExpenseGroupControllerIntegrationTest : IntegrationTestBase() {

    private fun asUser(username: String) = jwt().jwt { it.subject(username) }

    private fun createGroup(name: String, creator: String): Map<*, *> {
        val response = mockMvc.perform(
            post("/expense-groups")
                .with(asUser(creator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ExpenseGroupRequest(name)))
        ).andExpect(status().isCreated).andReturn()

        return objectMapper.readValue(response.response.contentAsString, Map::class.java)
    }

    @Test
    fun `createGroup without a token returns 401`() {
        mockMvc.perform(
            post("/expense-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ExpenseGroupRequest("Beach trip")))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `createGroup with a valid token returns 201`() {
        mockMvc.perform(
            post("/expense-groups")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ExpenseGroupRequest("Trip to the coast")))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Trip to the coast"))
            .andExpect(jsonPath("$.createdBy").value("alice"))
    }

    @Test
    fun `getGroup by a non-member returns 403`() {
        val group = createGroup("Private group", "alice")
        val groupId = (group["id"] as Number).toLong()

        mockMvc.perform(get("/expense-groups/$groupId").with(asUser("mallory")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `joinGroup with a valid code adds the member`() {
        val group = createGroup("Join by code", "alice")
        val inviteCode = group["inviteCode"] as String

        mockMvc.perform(
            post("/groups/join")
                .with(asUser("bob"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(JoinGroupRequest(inviteCode)))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.memberUsername").value("bob"))
    }

    @Test
    fun `joinGroup with an unknown code returns 404`() {
        mockMvc.perform(
            post("/groups/join")
                .with(asUser("bob"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(JoinGroupRequest("DOES-NOT-EXIST")))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `joinGroup when already a member returns 409`() {
        val group = createGroup("Already joined", "alice")
        val inviteCode = group["inviteCode"] as String

        mockMvc.perform(
            post("/groups/join")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(JoinGroupRequest(inviteCode)))
        ).andExpect(status().isConflict)
    }

    @Test
    fun `getInvitation without a token returns 401`() {
        val group = createGroup("Private invitation", "alice")
        val inviteCode = group["inviteCode"] as String

        mockMvc.perform(get("/invitations/$inviteCode"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `getInvitation with a token returns the invitation data`() {
        val group = createGroup("Visible invitation", "alice")
        val inviteCode = group["inviteCode"] as String

        mockMvc.perform(get("/invitations/$inviteCode").with(asUser("bob")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.groupName").value("Visible invitation"))
            .andExpect(jsonPath("$.invitedBy").value("alice"))
    }

    @Test
    fun `deleteGroup is blocked while there is an outstanding balance`() {
        val group = createGroup("Unsettled group", "alice")
        val groupId = (group["id"] as Number).toLong()
        val inviteCode = group["inviteCode"] as String

        mockMvc.perform(
            post("/groups/join")
                .with(asUser("bob"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(JoinGroupRequest(inviteCode)))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId = groupId,
                            description = "Dinner",
                            amount = BigDecimal("40.00"),
                            shares = listOf(
                                ExpenseShareRequest("alice", BigDecimal("20.00")),
                                ExpenseShareRequest("bob", BigDecimal("20.00"))
                            )
                        )
                    )
                )
        ).andExpect(status().isCreated)

        mockMvc.perform(delete("/expense-groups/$groupId").with(asUser("alice")))
            .andExpect(status().isConflict)
    }

    @Test
    fun `netting reduces cross debts to the minimum number of transfers`() {
        val group = createGroup("Netting trip", "alice")
        val groupId = (group["id"] as Number).toLong()
        val inviteCode = group["inviteCode"] as String

        for (member in listOf("bob", "carol")) {
            mockMvc.perform(
                post("/groups/join")
                    .with(asUser(member))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(JoinGroupRequest(inviteCode)))
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId = groupId,
                            description = "Case of beer",
                            amount = BigDecimal("60.00"),
                            shares = listOf(
                                ExpenseShareRequest("alice", BigDecimal("20.00")),
                                ExpenseShareRequest("bob", BigDecimal("20.00")),
                                ExpenseShareRequest("carol", BigDecimal("20.00"))
                            )
                        )
                    )
                )
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/expense-groups/$groupId/netting").with(asUser("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].toUsername").value("alice"))
            .andExpect(jsonPath("$[1].toUsername").value("alice"))
    }
}
