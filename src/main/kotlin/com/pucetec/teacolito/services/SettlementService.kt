package com.pucetec.teacolito.services

import com.pucetec.teacolito.dto.SettlementRequest
import com.pucetec.teacolito.dto.SettlementResponse
import com.pucetec.teacolito.entities.Settlement
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.InvalidAmountException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
import com.pucetec.teacolito.exceptions.NotSettlementOwnerException
import com.pucetec.teacolito.exceptions.SettlementNotFoundException
import com.pucetec.teacolito.mappers.toResponse
import com.pucetec.teacolito.repositories.ExpenseGroupRepository
import com.pucetec.teacolito.repositories.GroupMemberRepository
import com.pucetec.teacolito.repositories.SettlementRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val expenseGroupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {

    fun createSettlement(request: SettlementRequest, currentUsername: String): SettlementResponse {
        val group = expenseGroupRepository.findById(request.groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No existe un grupo con id ${request.groupId}") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(group.id, currentUsername)) {
            throw NotGroupMemberException("El usuario $currentUsername no pertenece al grupo ${group.id}")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw InvalidAmountException("El monto del pago debe ser mayor a cero")
        }

        val settlement = Settlement(
            group = group,
            fromUsername = currentUsername,
            toUsername = request.toUsername,
            amount = request.amount,
            settledAt = LocalDateTime.now()
        )

        return settlementRepository.save(settlement).toResponse()
    }

    fun getSettlementsByGroup(groupId: Long, currentUsername: String): List<SettlementResponse> {
        expenseGroupRepository.findById(groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No existe un grupo con id $groupId") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(groupId, currentUsername)) {
            throw NotGroupMemberException("El usuario $currentUsername no pertenece al grupo $groupId")
        }

        return settlementRepository.findByGroupId(groupId).map { it.toResponse() }
    }

    fun deleteSettlement(id: Long, currentUsername: String) {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { SettlementNotFoundException("No existe un pago con id $id") }

        if (settlement.fromUsername != currentUsername) {
            throw NotSettlementOwnerException("El usuario $currentUsername no es el dueño del pago $id")
        }

        settlementRepository.delete(settlement)
    }
}
