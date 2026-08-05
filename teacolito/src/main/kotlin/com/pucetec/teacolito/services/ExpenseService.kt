package com.pucetec.teacolito.services

import com.pucetec.teacolito.clients.UserClient
import com.pucetec.teacolito.dto.ExpenseRequest
import com.pucetec.teacolito.dto.ExpenseResponse
import com.pucetec.teacolito.entities.Expense
import com.pucetec.teacolito.entities.ExpenseShare
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.ExpenseNotFoundException
import com.pucetec.teacolito.exceptions.GroupClosedException
import com.pucetec.teacolito.exceptions.InvalidAmountException
import com.pucetec.teacolito.exceptions.NotExpenseOwnerException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
import com.pucetec.teacolito.exceptions.ShareMismatchException
import com.pucetec.teacolito.mappers.toResponse
import com.pucetec.teacolito.repositories.ExpenseGroupRepository
import com.pucetec.teacolito.repositories.ExpenseRepository
import com.pucetec.teacolito.repositories.ExpenseShareRepository
import com.pucetec.teacolito.repositories.GroupMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseShareRepository: ExpenseShareRepository,
    private val expenseGroupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userClient: UserClient
) {

    private val log = LoggerFactory.getLogger(ExpenseService::class.java)

    fun createExpense(request: ExpenseRequest, currentUsername: String, token: String = ""): ExpenseResponse {
        val group = expenseGroupRepository.findById(request.groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No group exists with id ${request.groupId}") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(group.id, currentUsername)) {
            throw NotGroupMemberException(
                "User ${userClient.resolveDisplayName(currentUsername, token)} does not belong to group ${group.id}"
            )
        }

        if (group.closed) {
            log.warn("event=expense.create.rejected | msg=Group is closed | groupId=${group.id}")
            throw GroupClosedException("Group ${group.id} is closed")
        }

        if (request.amount <= BigDecimal.ZERO) {
            log.warn("event=expense.create.rejected | msg=Invalid amount | groupId=${group.id} amount=${request.amount}")
            throw InvalidAmountException("Expense amount must be greater than zero")
        }

        validateShares(request)

        val expense = expenseRepository.save(
            Expense(
                group = group,
                payerUsername = currentUsername,
                description = request.description,
                amount = request.amount,
                spentAt = LocalDateTime.now()
            )
        )

        val shares = request.shares.map { shareRequest ->
            expenseShareRepository.save(
                ExpenseShare(
                    expense = expense,
                    debtorUsername = shareRequest.debtorUsername,
                    shareAmount = shareRequest.shareAmount
                )
            )
        }

        log.info("event=expense.created | msg=Expense created | expenseId=${expense.id} groupId=${group.id} amount=${expense.amount}")
        return expense.toResponse(shares)
    }

    fun getExpensesByGroup(groupId: Long, currentUsername: String, token: String = ""): List<ExpenseResponse> {
        expenseGroupRepository.findById(groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No group exists with id $groupId") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(groupId, currentUsername)) {
            throw NotGroupMemberException(
                "User ${userClient.resolveDisplayName(currentUsername, token)} does not belong to group $groupId"
            )
        }

        return expenseRepository.findByGroupId(groupId).map { expense ->
            expense.toResponse(expenseShareRepository.findByExpenseId(expense.id))
        }
    }

    fun updateExpense(id: Long, request: ExpenseRequest, currentUsername: String, token: String = ""): ExpenseResponse {
        val expense = expenseRepository.findById(id)
            .orElseThrow { ExpenseNotFoundException("No expense exists with id $id") }

        if (expense.payerUsername != currentUsername) {
            throw NotExpenseOwnerException(
                "User ${userClient.resolveDisplayName(currentUsername, token)} is not the owner of expense $id"
            )
        }

        if (request.amount <= BigDecimal.ZERO) {
            log.warn("event=expense.update.rejected | msg=Invalid amount | expenseId=$id amount=${request.amount}")
            throw InvalidAmountException("Expense amount must be greater than zero")
        }

        validateShares(request)

        val oldAmount = expense.amount
        expense.description = request.description
        expense.amount = request.amount

        expenseShareRepository.findByExpenseId(id).forEach { expenseShareRepository.delete(it) }

        val updatedExpense = expenseRepository.save(expense)

        val shares = request.shares.map { shareRequest ->
            expenseShareRepository.save(
                ExpenseShare(
                    expense = updatedExpense,
                    debtorUsername = shareRequest.debtorUsername,
                    shareAmount = shareRequest.shareAmount
                )
            )
        }

        log.info("event=expense.updated | msg=Expense updated | expenseId=$id oldAmount=$oldAmount newAmount=${request.amount}")
        return updatedExpense.toResponse(shares)
    }

    fun deleteExpense(id: Long, currentUsername: String, token: String = "") {
        val expense = expenseRepository.findById(id)
            .orElseThrow { ExpenseNotFoundException("No expense exists with id $id") }

        if (expense.payerUsername != currentUsername) {
            throw NotExpenseOwnerException(
                "User ${userClient.resolveDisplayName(currentUsername, token)} is not the owner of expense $id"
            )
        }

        expenseShareRepository.findByExpenseId(id).forEach { expenseShareRepository.delete(it) }
        expenseRepository.delete(expense)
        log.info("event=expense.deleted | msg=Expense deleted | expenseId=$id")
    }

    private fun validateShares(request: ExpenseRequest) {
        val sharesSum = request.shares.fold(BigDecimal.ZERO) { acc, share -> acc + share.shareAmount }
        if (sharesSum.compareTo(request.amount) != 0) {
            log.warn("event=expense.share_mismatch | msg=Shares do not match amount | sharesSum=$sharesSum amount=${request.amount}")
            throw ShareMismatchException(
                "The sum of the shares ($sharesSum) does not match the amount (${request.amount})"
            )
        }
    }
}
