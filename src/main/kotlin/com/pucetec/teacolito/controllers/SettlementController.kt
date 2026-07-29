package com.pucetec.teacolito.controllers

import com.pucetec.teacolito.dto.SettlementRequest
import com.pucetec.teacolito.dto.SettlementResponse
import com.pucetec.teacolito.services.SettlementService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class SettlementController(
    private val settlementService: SettlementService
) {

    @PostMapping("/settlements")
    fun createSettlement(
        @RequestBody request: SettlementRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<SettlementResponse> {
        val response = settlementService.createSettlement(request, jwt.extractUsername())
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/expense-groups/{id}/settlements")
    fun getSettlementsByGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<SettlementResponse>> {
        val response = settlementService.getSettlementsByGroup(id, jwt.extractUsername())
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/settlements/{id}")
    fun deleteSettlement(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Void> {
        settlementService.deleteSettlement(id, jwt.extractUsername())
        return ResponseEntity.noContent().build()
    }
}
