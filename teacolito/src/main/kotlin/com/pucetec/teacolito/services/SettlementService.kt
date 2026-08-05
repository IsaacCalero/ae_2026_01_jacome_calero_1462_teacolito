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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val expenseGroupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {

    private val log = LoggerFactory.getLogger(SettlementService::class.java)

    fun createSettlement(request: SettlementRequest, currentUsername: String): SettlementResponse {
        val group = expenseGroupRepository.findById(request.groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No group exists with id ${request.groupId}") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(group.id, currentUsername)) {
            throw NotGroupMemberException("User $currentUsername does not belong to group ${group.id}")
        }

        if (request.amount <= BigDecimal.ZERO) {
            log.warn("event=settlement.create.rejected | msg=Invalid amount | groupId=${group.id} amount=${request.amount}")
            throw InvalidAmountException("Settlement amount must be greater than zero")
        }

        val settlement = Settlement(
            group = group,
            fromUsername = currentUsername,
            toUsername = request.toUsername,
            amount = request.amount,
            settledAt = LocalDateTime.now()
        )

        val response = settlementRepository.save(settlement).toResponse()
        log.info("event=settlement.created | msg=Settlement created | settlementId=${response.id} groupId=${group.id} from=$currentUsername to=${request.toUsername} amount=${request.amount}")
        return response
    }

    fun getSettlementsByGroup(groupId: Long, currentUsername: String): List<SettlementResponse> {
        expenseGroupRepository.findById(groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No group exists with id $groupId") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(groupId, currentUsername)) {
            throw NotGroupMemberException("User $currentUsername does not belong to group $groupId")
        }

        return settlementRepository.findByGroupId(groupId).map { it.toResponse() }
    }

    fun deleteSettlement(id: Long, currentUsername: String) {
        val settlement = settlementRepository.findById(id)
            .orElseThrow { SettlementNotFoundException("No settlement exists with id $id") }

        if (settlement.fromUsername != currentUsername) {
            throw NotSettlementOwnerException("User $currentUsername is not the owner of settlement $id")
        }

        settlementRepository.delete(settlement)
        log.info("event=settlement.deleted | msg=Settlement deleted | settlementId=$id")
    }
}
