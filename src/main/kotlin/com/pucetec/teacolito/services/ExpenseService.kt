package com.pucetec.teacolito.services

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
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val expenseShareRepository: ExpenseShareRepository,
    private val expenseGroupRepository: ExpenseGroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {

    fun createExpense(request: ExpenseRequest, currentUsername: String): ExpenseResponse {
        val group = expenseGroupRepository.findById(request.groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No existe un grupo con id ${request.groupId}") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(group.id, currentUsername)) {
            throw NotGroupMemberException("El usuario $currentUsername no pertenece al grupo ${group.id}")
        }

        if (group.closed) {
            throw GroupClosedException("El grupo ${group.id} está cerrado")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw InvalidAmountException("El monto del gasto debe ser mayor a cero")
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

        return expense.toResponse(shares)
    }

    fun getExpensesByGroup(groupId: Long, currentUsername: String): List<ExpenseResponse> {
        expenseGroupRepository.findById(groupId)
            .orElseThrow { ExpenseGroupNotFoundException("No existe un grupo con id $groupId") }

        if (!groupMemberRepository.existsByGroupIdAndMemberUsername(groupId, currentUsername)) {
            throw NotGroupMemberException("El usuario $currentUsername no pertenece al grupo $groupId")
        }

        return expenseRepository.findByGroupId(groupId).map { expense ->
            expense.toResponse(expenseShareRepository.findByExpenseId(expense.id))
        }
    }

    fun updateExpense(id: Long, request: ExpenseRequest, currentUsername: String): ExpenseResponse {
        val expense = expenseRepository.findById(id)
            .orElseThrow { ExpenseNotFoundException("No existe un gasto con id $id") }

        if (expense.payerUsername != currentUsername) {
            throw NotExpenseOwnerException("El usuario $currentUsername no es el dueño del gasto $id")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw InvalidAmountException("El monto del gasto debe ser mayor a cero")
        }

        validateShares(request)

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

        return updatedExpense.toResponse(shares)
    }

    fun deleteExpense(id: Long, currentUsername: String) {
        val expense = expenseRepository.findById(id)
            .orElseThrow { ExpenseNotFoundException("No existe un gasto con id $id") }

        if (expense.payerUsername != currentUsername) {
            throw NotExpenseOwnerException("El usuario $currentUsername no es el dueño del gasto $id")
        }

        expenseShareRepository.findByExpenseId(id).forEach { expenseShareRepository.delete(it) }
        expenseRepository.delete(expense)
    }

    private fun validateShares(request: ExpenseRequest) {
        val sharesSum = request.shares.fold(BigDecimal.ZERO) { acc, share -> acc + share.shareAmount }
        if (sharesSum.compareTo(request.amount) != 0) {
            throw ShareMismatchException(
                "La suma de los repartos ($sharesSum) no coincide con el monto (${request.amount})"
            )
        }
    }
}
