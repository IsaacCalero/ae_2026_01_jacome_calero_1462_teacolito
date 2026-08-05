package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.IntegrationTestBase
import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.ExpenseRequest
import com.pucetec.teacolito.dto.ExpenseShareRequest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
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

    @Test
    fun `createExpense with an amount of zero returns 400`() {
        val groupId = createGroup("Zero amount group", "alice")

        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(groupId, "Nothing", BigDecimal("0.00"), listOf())
                    )
                )
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `createExpense on a closed group returns 409`() {
        val groupId = createGroup("Closing soon", "alice")
        mockMvc.perform(patch("/expense-groups/$groupId/close").with(asUser("alice")))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId, "Too late", BigDecimal("10.00"),
                            listOf(ExpenseShareRequest("alice", BigDecimal("10.00")))
                        )
                    )
                )
        ).andExpect(status().isConflict)
    }

    @Test
    fun `getExpensesByGroup lists the group's expenses`() {
        val groupId = createGroup("Listing group", "alice")
        mockMvc.perform(
            post("/expenses")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId, "Coffee", BigDecimal("5.00"),
                            listOf(ExpenseShareRequest("alice", BigDecimal("5.00")))
                        )
                    )
                )
        )

        mockMvc.perform(get("/expense-groups/$groupId/expenses").with(asUser("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    private fun createExpense(groupId: Long, payer: String, amount: BigDecimal): Long {
        val response = mockMvc.perform(
            post("/expenses")
                .with(asUser(payer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(groupId, "Something", amount, listOf(ExpenseShareRequest(payer, amount)))
                    )
                )
        ).andReturn()

        return (objectMapper.readValue(response.response.contentAsString, Map::class.java)["id"] as Number).toLong()
    }

    @Test
    fun `updateExpense by the payer updates it`() {
        val groupId = createGroup("Update group", "alice")
        val expenseId = createExpense(groupId, "alice", BigDecimal("10.00"))

        mockMvc.perform(
            put("/expenses/$expenseId")
                .with(asUser("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId, "Updated description", BigDecimal("15.00"),
                            listOf(ExpenseShareRequest("alice", BigDecimal("15.00")))
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.description").value("Updated description"))
    }

    @Test
    fun `updateExpense by a non-payer returns 403`() {
        val groupId = createGroup("Not your expense", "alice")
        val expenseId = createExpense(groupId, "alice", BigDecimal("10.00"))

        mockMvc.perform(
            put("/expenses/$expenseId")
                .with(asUser("bob"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        ExpenseRequest(
                            groupId, "Hijacked", BigDecimal("10.00"),
                            listOf(ExpenseShareRequest("alice", BigDecimal("10.00")))
                        )
                    )
                )
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `deleteExpense by the payer deletes it`() {
        val groupId = createGroup("Delete group", "alice")
        val expenseId = createExpense(groupId, "alice", BigDecimal("10.00"))

        mockMvc.perform(delete("/expenses/$expenseId").with(asUser("alice")))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `deleteExpense for an unknown id returns 404`() {
        mockMvc.perform(delete("/expenses/999999").with(asUser("alice")))
            .andExpect(status().isNotFound)
    }
}
