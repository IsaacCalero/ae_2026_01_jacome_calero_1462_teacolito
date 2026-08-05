package com.pucetec.teacolito.services

import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.JoinGroupRequest
import com.pucetec.teacolito.entities.Expense
import com.pucetec.teacolito.entities.ExpenseGroup
import com.pucetec.teacolito.entities.ExpenseShare
import com.pucetec.teacolito.entities.GroupMember
import com.pucetec.teacolito.entities.Settlement
import com.pucetec.teacolito.exceptions.DuplicateGroupNameException
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.InvitationNotFoundException
import com.pucetec.teacolito.exceptions.MemberAlreadyExistsException
import com.pucetec.teacolito.exceptions.NotGroupCreatorException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
import com.pucetec.teacolito.exceptions.OutstandingBalanceException
import com.pucetec.teacolito.exceptions.BlankFieldException
import com.pucetec.teacolito.repositories.ExpenseGroupRepository
import com.pucetec.teacolito.repositories.ExpenseRepository
import com.pucetec.teacolito.repositories.ExpenseShareRepository
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
class ExpenseGroupServiceTest {

    @Mock
    lateinit var expenseGroupRepository: ExpenseGroupRepository

    @Mock
    lateinit var groupMemberRepository: GroupMemberRepository

    @Mock
    lateinit var expenseRepository: ExpenseRepository

    @Mock
    lateinit var expenseShareRepository: ExpenseShareRepository

    @Mock
    lateinit var settlementRepository: SettlementRepository

    @InjectMocks
    lateinit var service: ExpenseGroupService

    private fun buildGroup(id: Long = 1L, createdBy: String = "alice"): ExpenseGroup {
        val group = ExpenseGroup(
            name = "Beach trip",
            inviteCode = "ABC123",
            createdBy = createdBy,
            createdAt = LocalDateTime.now(),
            closed = false
        )
        group.id = id
        return group
    }

    @Test
    fun `createGroup should throw BlankFieldException when name is blank`() {
        val request = ExpenseGroupRequest(name = "   ")

        assertThrows<BlankFieldException> {
            service.createGroup(request, "alice")
        }
    }

    @Test
    fun `createGroup should create the group and add the creator as a member when name is valid`() {
        val request = ExpenseGroupRequest(name = "Beach trip")
        whenever(expenseGroupRepository.findByInviteCode(any())).thenReturn(null)
        whenever(expenseGroupRepository.save(any())).thenAnswer { invocation ->
            val group = invocation.arguments[0] as ExpenseGroup
            group.id = 1L
            group
        }

        val response = service.createGroup(request, "alice")

        assertEquals("Beach trip", response.name)
        assertEquals("alice", response.createdBy)
        assertEquals(false, response.closed)
        verify(groupMemberRepository).save(any())
    }

    @Test
    fun `createGroup should throw DuplicateGroupNameException when creator already has a group with that name`() {
        val request = ExpenseGroupRequest(name = "Beach trip")
        whenever(expenseGroupRepository.existsByNameAndCreatedBy("Beach trip", "alice")).thenReturn(true)

        assertThrows<DuplicateGroupNameException> {
            service.createGroup(request, "alice")
        }
    }

    @Test
    fun `getGroup should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getGroup(1L, "alice")
        }
    }

    @Test
    fun `getGroup should throw NotGroupMemberException when user is not a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getGroup(1L, "bob")
        }
    }

    @Test
    fun `getGroup should return the group when user is a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)

        val response = service.getGroup(1L, "alice")

        assertEquals("Beach trip", response.name)
    }

    @Test
    fun `updateGroup should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.updateGroup(1L, ExpenseGroupRequest("New name"), "alice")
        }
    }

    @Test
    fun `updateGroup should throw NotGroupCreatorException when user is not the creator`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<NotGroupCreatorException> {
            service.updateGroup(1L, ExpenseGroupRequest("New name"), "bob")
        }
    }

    @Test
    fun `updateGroup should throw BlankFieldException when new name is blank`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<BlankFieldException> {
            service.updateGroup(1L, ExpenseGroupRequest("   "), "alice")
        }
    }

    @Test
    fun `updateGroup should update the name when data is valid`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(expenseGroupRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.updateGroup(1L, ExpenseGroupRequest("New name"), "alice")

        assertEquals("New name", response.name)
    }

    @Test
    fun `updateGroup should throw DuplicateGroupNameException when creator already has another group with the new name`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(expenseGroupRepository.existsByNameAndCreatedByAndIdNot("New name", "alice", 1L)).thenReturn(true)

        assertThrows<DuplicateGroupNameException> {
            service.updateGroup(1L, ExpenseGroupRequest("New name"), "alice")
        }
    }

    @Test
    fun `deleteGroup should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.deleteGroup(1L, "alice")
        }
    }

    @Test
    fun `deleteGroup should throw NotGroupCreatorException when user is not the creator`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<NotGroupCreatorException> {
            service.deleteGroup(1L, "bob")
        }
    }

    @Test
    fun `deleteGroup should delete the group when user is the creator`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        service.deleteGroup(1L, "alice")

        verify(expenseGroupRepository).delete(group)
    }

    @Test
    fun `deleteGroup should throw OutstandingBalanceException when a member has a nonzero balance`() {
        val group = buildGroup()
        val expense = Expense(
            group = group,
            payerUsername = "alice",
            description = "Dinner",
            amount = BigDecimal("100.00"),
            spentAt = LocalDateTime.now()
        )
        expense.id = 1L
        val shareBob = ExpenseShare(expense = expense, debtorUsername = "bob", shareAmount = BigDecimal("100.00"))
        shareBob.id = 1L

        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(expenseRepository.findByGroupId(1L)).thenReturn(listOf(expense))
        whenever(expenseShareRepository.findByExpenseId(1L)).thenReturn(listOf(shareBob))
        whenever(settlementRepository.findByGroupId(1L)).thenReturn(emptyList())

        assertThrows<OutstandingBalanceException> {
            service.deleteGroup(1L, "alice")
        }
    }

    @Test
    fun `joinGroup should throw InvitationNotFoundException when code does not exist`() {
        whenever(expenseGroupRepository.findByInviteCode("ZZZ999")).thenReturn(null)

        assertThrows<InvitationNotFoundException> {
            service.joinGroup(JoinGroupRequest("ZZZ999"), "bob")
        }
    }

    @Test
    fun `joinGroup should throw MemberAlreadyExistsException when user is already a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findByInviteCode("ABC123")).thenReturn(group)
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)

        assertThrows<MemberAlreadyExistsException> {
            service.joinGroup(JoinGroupRequest("ABC123"), "alice")
        }
    }

    @Test
    fun `joinGroup should add the member when the code is valid`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findByInviteCode("ABC123")).thenReturn(group)
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)
        whenever(groupMemberRepository.save(any())).thenAnswer { invocation ->
            val member = invocation.arguments[0] as GroupMember
            member.id = 10L
            member
        }

        val response = service.joinGroup(JoinGroupRequest("ABC123"), "bob")

        assertEquals("bob", response.memberUsername)
    }

    @Test
    fun `getMembers should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getMembers(1L, "alice")
        }
    }

    @Test
    fun `getMembers should throw NotGroupMemberException when user is not a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getMembers(1L, "bob")
        }
    }

    @Test
    fun `getMembers should return the list of members`() {
        val group = buildGroup()
        val member = GroupMember(group = group, memberUsername = "alice", joinedAt = LocalDateTime.now())
        member.id = 1L
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(groupMemberRepository.findByGroupId(1L)).thenReturn(listOf(member))

        val response = service.getMembers(1L, "alice")

        assertEquals(1, response.size)
        assertEquals("alice", response[0].memberUsername)
    }

    @Test
    fun `closeGroup should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.closeGroup(1L, "alice")
        }
    }

    @Test
    fun `closeGroup should throw NotGroupCreatorException when user is not the creator`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<NotGroupCreatorException> {
            service.closeGroup(1L, "bob")
        }
    }

    @Test
    fun `closeGroup should close the group when user is the creator`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(expenseGroupRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.closeGroup(1L, "alice")

        assertEquals(true, response.closed)
    }

    @Test
    fun `getBalances should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getBalances(1L, "alice")
        }
    }

    @Test
    fun `getBalances should throw NotGroupMemberException when user is not a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getBalances(1L, "bob")
        }
    }

    @Test
    fun `getBalances should calculate the net balance of each user`() {
        val group = buildGroup()
        val expense = Expense(
            group = group,
            payerUsername = "alice",
            description = "Dinner",
            amount = BigDecimal("100.00"),
            spentAt = LocalDateTime.now()
        )
        expense.id = 1L
        val shareAlice = ExpenseShare(expense = expense, debtorUsername = "alice", shareAmount = BigDecimal("50.00"))
        shareAlice.id = 1L
        val shareBob = ExpenseShare(expense = expense, debtorUsername = "bob", shareAmount = BigDecimal("50.00"))
        shareBob.id = 2L
        val settlement = Settlement(
            group = group,
            fromUsername = "bob",
            toUsername = "alice",
            amount = BigDecimal("20.00"),
            settledAt = LocalDateTime.now()
        )
        settlement.id = 1L

        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(expenseRepository.findByGroupId(1L)).thenReturn(listOf(expense))
        whenever(expenseShareRepository.findByExpenseId(1L)).thenReturn(listOf(shareAlice, shareBob))
        whenever(settlementRepository.findByGroupId(1L)).thenReturn(listOf(settlement))

        val response = service.getBalances(1L, "alice")

        val aliceBalance = response.first { it.username == "alice" }.netAmount
        val bobBalance = response.first { it.username == "bob" }.netAmount

        assertEquals(0, BigDecimal("30.00").compareTo(aliceBalance))
        assertEquals(0, BigDecimal("-30.00").compareTo(bobBalance))
    }

    @Test
    fun `getNetting should throw ExpenseGroupNotFoundException when group does not exist`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getNetting(1L, "alice")
        }
    }

    @Test
    fun `getNetting should throw NotGroupMemberException when user is not a member`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getNetting(1L, "bob")
        }
    }

    @Test
    fun `getNetting should calculate the minimum transfers`() {
        val group = buildGroup()
        val expense = Expense(
            group = group,
            payerUsername = "alice",
            description = "Dinner",
            amount = BigDecimal("60.00"),
            spentAt = LocalDateTime.now()
        )
        expense.id = 1L
        val shareAlice = ExpenseShare(expense = expense, debtorUsername = "alice", shareAmount = BigDecimal("20.00"))
        shareAlice.id = 1L
        val shareBob = ExpenseShare(expense = expense, debtorUsername = "bob", shareAmount = BigDecimal("20.00"))
        shareBob.id = 2L
        val shareCarol = ExpenseShare(expense = expense, debtorUsername = "carol", shareAmount = BigDecimal("20.00"))
        shareCarol.id = 3L

        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(expenseRepository.findByGroupId(1L)).thenReturn(listOf(expense))
        whenever(expenseShareRepository.findByExpenseId(1L)).thenReturn(listOf(shareAlice, shareBob, shareCarol))
        whenever(settlementRepository.findByGroupId(1L)).thenReturn(emptyList())

        val transfers = service.getNetting(1L, "alice")

        assertEquals(2, transfers.size)
        transfers.forEach { transfer ->
            assertEquals("alice", transfer.toUsername)
            assertEquals(0, BigDecimal("20.00").compareTo(transfer.amount))
        }
    }

    @Test
    fun `getInvitation should throw InvitationNotFoundException when code does not exist`() {
        whenever(expenseGroupRepository.findByInviteCode("XYZ")).thenReturn(null)

        assertThrows<InvitationNotFoundException> {
            service.getInvitation("XYZ")
        }
    }

    @Test
    fun `getInvitation should return the invitation data when code exists`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findByInviteCode("ABC123")).thenReturn(group)
        whenever(groupMemberRepository.countByGroupId(1L)).thenReturn(3L)

        val response = service.getInvitation("ABC123")

        assertEquals("Beach trip", response.groupName)
        assertEquals("alice", response.invitedBy)
        assertEquals(3L, response.memberCount)
    }
}
