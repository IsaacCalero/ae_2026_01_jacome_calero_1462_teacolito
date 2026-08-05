package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.IntegrationTestBase
import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.ExpenseRequest
import com.pucetec.teacolito.dto.ExpenseShareRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

class ExpenseControllerIntegrationTest : IntegrationTestBase() {

    private fun asUser(username: String) = jwt().jwt { it.subject(username) }

    private fun createGroup(name: String, creator: String): Long {
        val response = mockMvc.perform(
            post("/expense-groups")
                .with(asUser(creator))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ExpenseGroupRequest(name)))
        ).andReturn()

        return (objectMapper.readValue(response.response.contentAsString, Map::class.java)["id"] as Number).toLong()
    }

    @Test
    fun `createExpense without a token returns 401`() {
        mockMvc.perform(
            post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(1L, "Dinner", BigDecimal("10.00"), listOf())
                    )
                )
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `createExpense whose shares do not match the amount returns 400`() {
        val groupId = createGroup("Mismatch group", "alice")

        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId = groupId,
                            description = "Dinner",
                            amount = BigDecimal("100.00"),
                            shares = listOf(ExpenseShareRequest("alice", BigDecimal("40.00")))
                        )
                    )
                )
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `createExpense with matching shares returns 201`() {
        val groupId = createGroup("Valid expense group", "alice")

        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId = groupId,
                            description = "Groceries",
                            amount = BigDecimal("30.00"),
                            shares = listOf(ExpenseShareRequest("alice", BigDecimal("30.00")))
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.payerUsername").value("alice"))
            .andExpect(jsonPath("$.shares.length()").value(1))
    }
}
