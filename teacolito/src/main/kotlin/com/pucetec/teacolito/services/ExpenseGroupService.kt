package com.pucetec.teacolito.services

import com.pucetec.teacolito.dto.BalanceResponse
import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.ExpenseGroupResponse
import com.pucetec.teacolito.dto.JoinGroupRequest
import com.pucetec.teacolito.dto.GroupMemberResponse
import com.pucetec.teacolito.dto.InvitationResponse
import com.pucetec.teacolito.dto.TransferResponse
import com.pucetec.teacolito.entities.ExpenseGroup
import com.pucetec.teacolito.entities.GroupMember
import com.pucetec.teacolito.exceptions.BlankFieldException
import com.pucetec.teacolito.exceptions.DuplicateGroupNameException
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.InvitationNotFoundException
import com.pucetec.teacolito.exceptions.MemberAlreadyExistsException
import com.pucetec.teacolito.exceptions.NotGroupCreatorException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
import com.pucetec.teacolito.exceptions.OutstandingBalanceException
import com.pucetec.teacolito.mappers.toResponse
import com.pucetec.teacolito.repositories.ExpenseGroupRepository
import com.pucetec.teacolito.repositories.ExpenseRepository
import com.pucetec.teacolito.repositories.ExpenseShareRepository
import com.pucetec.teacolito.repositories.GroupMemberRepository
import com.pucetec.teacolito.repositories.SettlementRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class ExpenseGroupService(
    private val expenseGroupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val expenseShareRepository: ExpenseShareRepository,
    private val settlementRepository: SettlementRepository
) {

    fun createGroup(request: ExpenseGroupRequest, currentUsername: String): ExpenseGroupResponse {
        if (request.name.isBlank()) {
            throw BlankFieldException("Group name cannot be blank")
        }

        if (expenseGroupRepository.existsByNameAndCreatedBy(request.name, currentUsername)) {
            throw DuplicateGroupNameException("User $currentUsername already has a group named '${request.name}'")
        }

        val group = expenseGroupRepository.save(
            ExpenseGroup(
                name = request.name,
                inviteCode = generateUniqueInviteCode(),
                createdBy = currentUsername,
                createdAt = LocalDateTime.now(),
                closed = false
            )
        )

        groupMemberRepository.save(
            GroupMember(
                group = group,
                memberUsername = currentUsername,
                joinedAt = LocalDateTime.now()
            )
        )

        return group.toResponse()
    }

    fun getGroup(groupId: Long, currentUsername: String): ExpenseGroupResponse {
        val group = findGroupOrThrow(groupId)
        requireMember(groupId, currentUsername)
        return group.toResponse()
    }

    fun updateGroup(groupId: Long, request: ExpenseGroupRequest, currentUsername: String): ExpenseGroupResponse {
        val group = findGroupOrThrow(groupId)
        requireCreator(group, currentUsername)

        if (request.name.isBlank()) {
            throw BlankFieldException("Group name cannot be blank")
        }

        if (expenseGroupRepository.existsByNameAndCreatedByAndIdNot(request.name, currentUsername, groupId)) {
            throw DuplicateGroupNameException("User $currentUsername already has a group named '${request.name}'")
        }

        group.name = request.name
        return expenseGroupRepository.save(group).toResponse()
    }

    fun deleteGroup(groupId: Long, currentUsername: String) {
        val group = findGroupOrThrow(groupId)
        requireCreator(group, currentUsername)

        val hasOutstandingBalance = computeBalances(groupId).any { it.netAmount.compareTo(BigDecimal.ZERO) != 0 }
        if (hasOutstandingBalance) {
            throw OutstandingBalanceException("Group $groupId cannot be deleted while members have outstanding balances")
        }

        expenseGroupRepository.delete(group)
    }

    fun joinGroup(request: JoinGroupRequest, currentUsername: String): GroupMemberResponse {
        val group = expenseGroupRepository.findByInviteCode(request.code)
            ?: throw InvitationNotFoundException("No invitation exists with code ${request.code}")

        if (groupMemberRepository.existsByGroupIdAndMemberUsername(group.id, currentUsername)) {
            throw MemberAlreadyExistsException("User $currentUsername is already a member of group ${group.id}")
        }

        val member = groupMemberRepository.save(
            GroupMember(
                group = group,
                memberUsername = currentUsername,
                joinedAt = LocalDateTime.now()
            )
        )

        return member.toResponse()
    }

    fun getMembers(groupId: Long, currentUsername: String): List<GroupMemberResponse> {
        findGroupOrThrow(groupId)
        requireMember(groupId, currentUsername)
        return groupMemberRepository.findByGroupId(groupId).map { it.toResponse() }
    }

    fun closeGroup(groupId: Long, currentUsername: String): ExpenseGroupResponse {
        val group = findGroupOrThrow(groupId)
        requireCreator(group, currentUsername)
        group.closed = true
        return expenseGroupRepository.save(group).toResponse()
    }

    fun getBalances(groupId: Long, currentUsername: String): List<BalanceResponse> {
        findGroupOrThrow(groupId)
        requireMember(groupId, currentUsername)
        return computeBalances(groupId)
    }

    fun getNetting(groupId: Long, currentUsername: String): List<TransferResponse> {
        findGroupOrThrow(groupId)
        requireMember(groupId, currentUsername)

        val balances = computeBalances(groupId)

        val creditors = balances
            .filter { it.netAmount > BigDecimal.ZERO }
            .map { it.username to it.netAmount }
            .toMutableList()

        val debtors = balances
            .filter { it.netAmount < BigDecimal.ZERO }
            .map { it.username to it.netAmount.negate() }
            .toMutableList()

        val transfers = mutableListOf<TransferResponse>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortByDescending { it.second }
            creditors.sortByDescending { it.second }

            val (debtorUsername, debtAmount) = debtors[0]
            val (creditorUsername, creditAmount) = creditors[0]

            val transferAmount = debtAmount.min(creditAmount)
            transfers.add(
                TransferResponse(fromUsername = debtorUsername, toUsername = creditorUsername, amount = transferAmount)
            )

            val remainingDebt = debtAmount - transferAmount
            val remainingCredit = creditAmount - transferAmount

            debtors.removeAt(0)
            if (remainingDebt > BigDecimal.ZERO) {
                debtors.add(0, debtorUsername to remainingDebt)
            }

            creditors.removeAt(0)
            if (remainingCredit > BigDecimal.ZERO) {
                creditors.add(0, creditorUsername to remainingCredit)
            }
        }

        return transfers
    }

    fun getInvitation(code: String): InvitationResponse {
        val group = expenseGroupRepository.findByInviteCode(code)
            ?: throw InvitationNotFoundException("No invitation exists with code $code")

        val memberCount = groupMemberRepository.countByGroupId(group.id)

        return InvitationResponse(
            groupName = group.name,
            invitedBy = group.createdBy,
            memberCount = memberCount
        )
    }

    private fun computeBalances(groupId: Long): List<BalanceResponse> {
        val expenses = expenseRepository.findByGroupId(groupId)
        val settlements = settlementRepository.findByGroupId(groupId)
        val shares = expenses.flatMap { expenseShareRepository.findByExpenseId(it.id) }

        val netAmounts = linkedMapOf<String, BigDecimal>()

        expenses.forEach { expense ->
            netAmounts.merge(expense.payerUsername, expense.amount, BigDecimal::add)
        }
        shares.forEach { share ->
            netAmounts.merge(share.debtorUsername, share.shareAmount.negate(), BigDecimal::add)
        }
        settlements.forEach { settlement ->
            netAmounts.merge(settlement.fromUsername, settlement.amount, BigDecimal::add)
            netAmounts.merge(settlement.toUsername, settlement.amount.negate(), BigDecimal::add)
        }

        return netAmounts.map { (username, amount) -> BalanceResponse(username = username, netAmount = amount) }
    }

    private fun findGroupOrThrow(groupId: Long): ExpenseGroup =
        expenseGroupRepository.findById(groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No group exists with id $groupId") }

    private fun requireMember(groupId: Long, username: String) {
        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(groupId, username)) {
            throw NotGroupMemberException("User $username does not belong to group $groupId")
        }
    }

    private fun requireCreator(group: ExpenseGroup, username: String) {
        if (group.createdBy != username) {
            throw NotGroupCreatorException("User $username is not the creator of group ${group.id}")
        }
    }

    private fun generateUniqueInviteCode(): String {
        var code: String
        do {
            code = UUID.randomUUID().toString().substring(0, 8).uppercase()
        } while (expenseGroupRepository.findByInviteCode(code) != null)
        return code
    }
}
