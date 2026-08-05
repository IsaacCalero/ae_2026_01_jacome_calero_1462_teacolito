package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.clients.UserClient
import com.pucetec.teacolito.clients.enrich
import com.pucetec.teacolito.clients.enrichBalances
import com.pucetec.teacolito.clients.enrichMembers
import com.pucetec.teacolito.clients.enrichTransfers
import com.pucetec.teacolito.dto.BalanceResponse
import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.ExpenseGroupResponse
import com.pucetec.teacolito.dto.GroupMemberResponse
import com.pucetec.teacolito.dto.InvitationResponse
import com.pucetec.teacolito.dto.JoinGroupRequest
import com.pucetec.teacolito.dto.TransferResponse
import com.pucetec.teacolito.services.ExpenseGroupService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ExpenseGroupController(
    private val expenseGroupService: ExpenseGroupService,
    private val userClient: UserClient
) {

    @PostMapping("/expense-groups")
    fun createGroup(
        @RequestBody request: ExpenseGroupRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ExpenseGroupResponse> {
        val response = expenseGroupService.createGroup(request, jwt.extractUsername())
        return ResponseEntity.status(HttpStatus.CREATED).body(response.enrich(userClient, jwt.tokenValue))
    }

    @GetMapping("/expense-groups/{id}")
    fun getGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ExpenseGroupResponse> {
        val response = expenseGroupService.getGroup(id, jwt.extractUsername())
        return ResponseEntity.ok(response.enrich(userClient, jwt.tokenValue))
    }

    @PutMapping("/expense-groups/{id}")
    fun updateGroup(
        @PathVariable id: Long,
        @RequestBody request: ExpenseGroupRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ExpenseGroupResponse> {
        val response = expenseGroupService.updateGroup(id, request, jwt.extractUsername())
        return ResponseEntity.ok(response.enrich(userClient, jwt.tokenValue))
    }

    @DeleteMapping("/expense-groups/{id}")
    fun deleteGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        expenseGroupService.deleteGroup(id, jwt.extractUsername())
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/expense-groups/{id}/close")
    fun closeGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<ExpenseGroupResponse> {
        val response = expenseGroupService.closeGroup(id, jwt.extractUsername())
        return ResponseEntity.ok(response.enrich(userClient, jwt.tokenValue))
    }

    @PostMapping("/groups/join")
    fun joinGroup(
        @RequestBody request: JoinGroupRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<GroupMemberResponse> {
        val response = expenseGroupService.joinGroup(request, jwt.extractUsername())
        return ResponseEntity.status(HttpStatus.CREATED).body(response.enrich(userClient, jwt.tokenValue))
    }

    @GetMapping("/expense-groups/{id}/members")
    fun getMembers(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<GroupMemberResponse>> {
        val response = expenseGroupService.getMembers(id, jwt.extractUsername())
        return ResponseEntity.ok(response.enrichMembers(userClient, jwt.tokenValue))
    }

    @GetMapping("/expense-groups/{id}/balances")
    fun getBalances(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<BalanceResponse>> {
        val response = expenseGroupService.getBalances(id, jwt.extractUsername())
        return ResponseEntity.ok(response.enrichBalances(userClient, jwt.tokenValue))
    }

    @GetMapping("/expense-groups/{id}/netting")
    fun getNetting(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<TransferResponse>> {
        val response = expenseGroupService.getNetting(id, jwt.extractUsername())
        return ResponseEntity.ok(response.enrichTransfers(userClient, jwt.tokenValue))
    }

    @GetMapping("/invitations/{code}")
    fun getInvitation(
        @PathVariable code: String,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<InvitationResponse> {
        val response = expenseGroupService.getInvitation(code)
        return ResponseEntity.ok(response.enrich(userClient, jwt.tokenValue))
    }
}
