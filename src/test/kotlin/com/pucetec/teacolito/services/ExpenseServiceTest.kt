package com.pucetec.teacolito.services

import com.pucetec.teacolito.dto.ExpenseRequest
import com.pucetec.teacolito.dto.ExpenseShareRequest
import com.pucetec.teacolito.entities.Expense
import com.pucetec.teacolito.entities.ExpenseGroup
import com.pucetec.teacolito.entities.ExpenseShare
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.ExpenseNotFoundException
import com.pucetec.teacolito.exceptions.GroupClosedException
import com.pucetec.teacolito.exceptions.InvalidAmountException
import com.pucetec.teacolito.exceptions.NotExpenseOwnerException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
import com.pucetec.teacolito.exceptions.ShareMismatchException
import com.pucetec.teacolito.repositories.ExpenseGroupRepository
import com.pucetec.teacolito.repositories.ExpenseRepository
import com.pucetec.teacolito.repositories.ExpenseShareRepository
import com.pucetec.teacolito.repositories.GroupMemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ExpenseServiceTest {

    @Mock
    lateinit var expenseRepository: ExpenseRepository

    @Mock
    lateinit var expenseShareRepository: ExpenseShareRepository

    @Mock
    lateinit var expenseGroupRepository: ExpenseGroupRepository

    @Mock
    lateinit var groupMemberRepository: GroupMemberRepository

    @InjectMocks
    lateinit var service: ExpenseService

    private fun buildGroup(closed: Boolean = false): ExpenseGroup {
        val group = ExpenseGroup(
            name = "Viaje a la playa",
            inviteCode = "ABC123",
            createdBy = "alice",
            createdAt = LocalDateTime.now(),
            closed = closed
        )
        group.id = 1L
        return group
    }

    private fun buildExpense(group: ExpenseGroup, payer: String = "alice"): Expense {
        val expense = Expense(
            group = group,
            payerUsername = payer,
            description = "Cena",
            amount = BigDecimal("100.00"),
            spentAt = LocalDateTime.now()
        )
        expense.id = 1L
        return expense
    }

    @Test
    fun `createExpense deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        val request = ExpenseRequest(1L, "Cena", BigDecimal("100.00"), listOf())

        assertThrows<ExpenseGroupNotFoundException> {
            service.createExpense(request, "alice")
        }
    }

    @Test
    fun `createExpense deberia lanzar NotGroupMemberException cuando el usuario no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "carol")).thenReturn(false)

        val request = ExpenseRequest(1L, "Cena", BigDecimal("100.00"), listOf())

        assertThrows<NotGroupMemberException> {
            service.createExpense(request, "carol")
        }
    }

    @Test
    fun `createExpense deberia lanzar GroupClosedException cuando el grupo esta cerrado`() {
        val group = buildGroup(closed = true)
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)

        val request = ExpenseRequest(1L, "Cena", BigDecimal("100.00"), listOf())

        assertThrows<GroupClosedException> {
            service.createExpense(request, "alice")
        }
    }

    @Test
    fun `createExpense deberia lanzar InvalidAmountException cuando el monto es cero o negativo`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)

        val request = ExpenseRequest(1L, "Cena", BigDecimal("0.00"), listOf())

        assertThrows<InvalidAmountException> {
            service.createExpense(request, "alice")
        }
    }

    @Test
    fun `createExpense deberia lanzar ShareMismatchException cuando la suma de shares no coincide con el monto`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)

        val request = ExpenseRequest(
            1L, "Cena", BigDecimal("100.00"),
            listOf(ExpenseShareRequest("bob", BigDecimal("40.00")))
        )

        assertThrows<ShareMismatchException> {
            service.createExpense(request, "alice")
        }
    }

    @Test
    fun `createExpense deberia crear el gasto y sus repartos cuando los datos son validos`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(expenseRepository.save(any())).thenAnswer {
            val expense = it.arguments[0] as Expense
            expense.id = 1L
            expense
        }
        whenever(expenseShareRepository.save(any())).thenAnswer {
            val share = it.arguments[0] as ExpenseShare
            share.id = 1L
            share
        }

        val request = ExpenseRequest(
            1L, "Cena", BigDecimal("100.00"),
            listOf(
                ExpenseShareRequest("alice", BigDecimal("50.00")),
                ExpenseShareRequest("bob", BigDecimal("50.00"))
            )
        )

        val response = service.createExpense(request, "alice")

        assertEquals("alice", response.payerUsername)
        assertEquals(2, response.shares.size)
    }

    @Test
    fun `getExpensesByGroup deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getExpensesByGroup(1L, "alice")
        }
    }

    @Test
    fun `getExpensesByGroup deberia lanzar NotGroupMemberException cuando el usuario no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "carol")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getExpensesByGroup(1L, "carol")
        }
    }

    @Test
    fun `getExpensesByGroup deberia retornar los gastos del grupo`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(expenseRepository.findByGroupId(1L)).thenReturn(listOf(expense))
        whenever(expenseShareRepository.findByExpenseId(1L)).thenReturn(listOf())

        val response = service.getExpensesByGroup(1L, "alice")

        assertEquals(1, response.size)
    }

    @Test
    fun `updateExpense deberia lanzar ExpenseNotFoundException cuando el gasto no existe`() {
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.empty())

        val request = ExpenseRequest(1L, "Cena", BigDecimal("100.00"), listOf())

        assertThrows<ExpenseNotFoundException> {
            service.updateExpense(1L, request, "alice")
        }
    }

    @Test
    fun `updateExpense deberia lanzar NotExpenseOwnerException cuando el usuario no es el pagador`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        val request = ExpenseRequest(1L, "Cena", BigDecimal("100.00"), listOf())

        assertThrows<NotExpenseOwnerException> {
            service.updateExpense(1L, request, "bob")
        }
    }

    @Test
    fun `updateExpense deberia lanzar InvalidAmountException cuando el monto es cero o negativo`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        val request = ExpenseRequest(1L, "Cena", BigDecimal("0.00"), listOf())

        assertThrows<InvalidAmountException> {
            service.updateExpense(1L, request, "alice")
        }
    }

    @Test
    fun `updateExpense deberia lanzar ShareMismatchException cuando la suma de shares no coincide con el monto`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        val request = ExpenseRequest(
            1L, "Cena", BigDecimal("100.00"),
            listOf(ExpenseShareRequest("bob", BigDecimal("40.00")))
        )

        assertThrows<ShareMismatchException> {
            service.updateExpense(1L, request, "alice")
        }
    }

    @Test
    fun `updateExpense deberia actualizar el gasto y sus repartos cuando los datos son validos`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))
        whenever(expenseShareRepository.findByExpenseId(1L)).thenReturn(listOf())
        whenever(expenseRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(expenseShareRepository.save(any())).thenAnswer {
            val share = it.arguments[0] as ExpenseShare
            share.id = 2L
            share
        }

        val request = ExpenseRequest(
            1L, "Cena actualizada", BigDecimal("80.00"),
            listOf(ExpenseShareRequest("bob", BigDecimal("80.00")))
        )

        val response = service.updateExpense(1L, request, "alice")

        assertEquals("Cena actualizada", response.description)
        assertEquals(0, BigDecimal("80.00").compareTo(response.amount))
    }

    @Test
    fun `deleteExpense deberia lanzar ExpenseNotFoundException cuando el gasto no existe`() {
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseNotFoundException> {
            service.deleteExpense(1L, "alice")
        }
    }

    @Test
    fun `deleteExpense deberia lanzar NotExpenseOwnerException cuando el usuario no es el pagador`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))

        assertThrows<NotExpenseOwnerException> {
            service.deleteExpense(1L, "bob")
        }
    }

    @Test
    fun `deleteExpense deberia eliminar el gasto y sus repartos cuando el usuario es el pagador`() {
        val group = buildGroup()
        val expense = buildExpense(group)
        whenever(expenseRepository.findById(1L)).thenReturn(Optional.of(expense))
        whenever(expenseShareRepository.findByExpenseId(1L)).thenReturn(listOf())

        service.deleteExpense(1L, "alice")

        verify(expenseRepository).delete(expense)
    }
}
