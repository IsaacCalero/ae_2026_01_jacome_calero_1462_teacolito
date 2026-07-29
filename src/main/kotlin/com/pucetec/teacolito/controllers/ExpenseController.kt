package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.dto.ExpenseRequest
import com.pucetec.teacolito.dto.ExpenseResponse
import com.pucetec.teacolito.services.ExpenseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ExpenseController(
    private val expenseService: ExpenseService
) {

    @PostMapping("/expenses")
    fun createExpense(
        @RequestBody request: ExpenseRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ExpenseResponse> {
        val response = expenseService.createExpense(request, jwt.extractUsername())
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/expense-groups/{id}/expenses")
    fun getExpensesByGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<ExpenseResponse>> {
        val response = expenseService.getExpensesByGroup(id, jwt.extractUsername())
        return ResponseEntity.ok(response)
    }

    @PutMapping("/expenses/{id}")
    fun updateExpense(
        @PathVariable id: Long,
        @RequestBody request: ExpenseRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ExpenseResponse> {
        val response = expenseService.updateExpense(id, request, jwt.extractUsername())
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/expenses/{id}")
    fun deleteExpense(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        expenseService.deleteExpense(id, jwt.extractUsername())
        return ResponseEntity.noContent().build()
    }
}
