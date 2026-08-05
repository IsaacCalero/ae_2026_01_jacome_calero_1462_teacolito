package com.pucetec.teacolito.services

import com.pucetec.teacolito.dto.SettlementRequest
import com.pucetec.teacolito.entities.ExpenseGroup
import com.pucetec.teacolito.entities.Settlement
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.InvalidAmountException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
import com.pucetec.teacolito.exceptions.NotSettlementOwnerException
import com.pucetec.teacolito.exceptions.SettlementNotFoundException
import com.pucetec.teacolito.repositories.ExpenseGroupRepository
import com.pucetec.teacolito.repositories.GroupMemberRepository
import com.pucetec.teacolito.repositories.SettlementRepository
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
class SettlementServiceTest {

    @Mock
    lateinit var settlementRepository: SettlementRepository

    @Mock
    lateinit var expenseGroupRepository: ExpenseGroupRepository

    @Mock
    lateinit var groupMemberRepository: GroupMemberRepository

    @InjectMocks
    lateinit var service: SettlementService

    private fun buildGroup(): ExpenseGroup {
        val group = ExpenseGroup(
            name = "Beach trip",
            inviteCode = "ABC123",
            createdBy = "alice",
            createdAt = LocalDateTime.now(),
            closed = false
        )
        group.id = 1L
        return group
    }

    private fun buildSettlement(group: ExpenseGroup): Settlement {
        val settlement = Settlement(
            group = group,
            fromUsername = "bob",
            toUsername = "alice",
            amount = BigDecimal("20.00"),
            settledAt = LocalDateTime.now()
        )
        settlement.id = 1L
        return settlement
    }

    @Test
    fun `createSettlement should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        val request = SettlementRequest(1L, "alice", BigDecimal("20.00"))

        assertThrows<ExpenseGroupNotFoundException> {
            service.createSettlement(request, "bob")
        }
    }

    @Test
    fun `createSettlement should throw NotGroupMemberException when user is not a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "carol")).thenReturn(false)

        val request = SettlementRequest(1L, "alice", BigDecimal("20.00"))

        assertThrows<NotGroupMemberException> {
            service.createSettlement(request, "carol")
        }
    }

    @Test
    fun `createSettlement should throw InvalidAmountException when amount is zero or negative`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(true)

        val request = SettlementRequest(1L, "alice", BigDecimal("0.00"))

        assertThrows<InvalidAmountException> {
            service.createSettlement(request, "bob")
        }
    }

    @Test
    fun `createSettlement should register the payment when data is valid`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(true)
        whenever(settlementRepository.save(any())).thenAnswer {
            val settlement = it.arguments[0] as Settlement
            settlement.id = 1L
            settlement
        }

        val request = SettlementRequest(1L, "alice", BigDecimal("20.00"))

        val response = service.createSettlement(request, "bob")

        assertEquals("bob", response.fromUsername)
        assertEquals("alice", response.toUsername)
    }

    @Test
    fun `getSettlementsByGroup should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getSettlementsByGroup(1L, "bob")
        }
    }

    @Test
    fun `getSettlementsByGroup should throw NotGroupMemberException when user is not a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "carol")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getSettlementsByGroup(1L, "carol")
        }
    }

    @Test
    fun `getSettlementsByGroup should return the group's settlements`() {
        val group = buildGroup()
        val settlement = buildSettlement(group)
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(true)
        whenever(settlementRepository.findByGroupId(1L)).thenReturn(listOf(settlement))

        val response = service.getSettlementsByGroup(1L, "bob")

        assertEquals(1, response.size)
    }

    @Test
    fun `deleteSettlement should throw SettlementNotFoundException when settlement does not exist`() {
        whenever(settlementRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<SettlementNotFoundException> {
            service.deleteSettlement(1L, "bob")
        }
    }

    @Test
    fun `deleteSettlement should throw NotSettlementOwnerException when user is not the one who paid`() {
        val group = buildGroup()
        val settlement = buildSettlement(group)
        whenever(settlementRepository.findById(1L)).thenReturn(Optional.of(settlement))

        assertThrows<NotSettlementOwnerException> {
            service.deleteSettlement(1L, "alice")
        }
    }

    @Test
    fun `deleteSettlement should delete the settlement when user is the one who registered it`() {
        val group = buildGroup()
        val settlement = buildSettlement(group)
        whenever(settlementRepository.findById(1L)).thenReturn(Optional.of(settlement))

        service.deleteSettlement(1L, "bob")

        verify(settlementRepository).delete(settlement)
    }
}
