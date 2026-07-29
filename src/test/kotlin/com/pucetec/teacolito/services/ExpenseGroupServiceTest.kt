package com.pucetec.teacolito.services

import com.pucetec.teacolito.dto.ExpenseGroupRequest
import com.pucetec.teacolito.dto.GroupMemberRequest
import com.pucetec.teacolito.entities.Expense
import com.pucetec.teacolito.entities.ExpenseGroup
import com.pucetec.teacolito.entities.ExpenseShare
import com.pucetec.teacolito.entities.GroupMember
import com.pucetec.teacolito.entities.Settlement
import com.pucetec.teacolito.exceptions.ExpenseGroupNotFoundException
import com.pucetec.teacolito.exceptions.InvitationNotFoundException
import com.pucetec.teacolito.exceptions.MemberAlreadyExistsException
import com.pucetec.teacolito.exceptions.NotGroupCreatorException
import com.pucetec.teacolito.exceptions.NotGroupMemberException
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
            name = "Viaje a la playa",
            inviteCode = "ABC123",
            createdBy = createdBy,
            createdAt = LocalDateTime.now(),
            closed = false
        )
        group.id = id
        return group
    }

    @Test
    fun `createGroup deberia lanzar BlankFieldException cuando el nombre esta en blanco`() {
        val request = ExpenseGroupRequest(name = "   ")

        assertThrows<BlankFieldException> {
            service.createGroup(request, "alice")
        }
    }

    @Test
    fun `createGroup deberia crear el grupo y agregar al creador como miembro cuando el nombre es valido`() {
        val request = ExpenseGroupRequest(name = "Viaje a la playa")
        whenever(expenseGroupRepository.findByInviteCode(any())).thenReturn(null)
        whenever(expenseGroupRepository.save(any())).thenAnswer { invocation ->
            val group = invocation.arguments[0] as ExpenseGroup
            group.id = 1L
            group
        }

        val response = service.createGroup(request, "alice")

        assertEquals("Viaje a la playa", response.name)
        assertEquals("alice", response.createdBy)
        assertEquals(false, response.closed)
        verify(groupMemberRepository).save(any())
    }

    @Test
    fun `getGroup deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getGroup(1L, "alice")
        }
    }

    @Test
    fun `getGroup deberia lanzar NotGroupMemberException cuando el usuario no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getGroup(1L, "bob")
        }
    }

    @Test
    fun `getGroup deberia retornar el grupo cuando el usuario es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)

        val response = service.getGroup(1L, "alice")

        assertEquals("Viaje a la playa", response.name)
    }

    @Test
    fun `updateGroup deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.updateGroup(1L, ExpenseGroupRequest("Nuevo nombre"), "alice")
        }
    }

    @Test
    fun `updateGroup deberia lanzar NotGroupCreatorException cuando el usuario no es el creador`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<NotGroupCreatorException> {
            service.updateGroup(1L, ExpenseGroupRequest("Nuevo nombre"), "bob")
        }
    }

    @Test
    fun `updateGroup deberia lanzar BlankFieldException cuando el nuevo nombre esta en blanco`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<BlankFieldException> {
            service.updateGroup(1L, ExpenseGroupRequest("   "), "alice")
        }
    }

    @Test
    fun `updateGroup deberia actualizar el nombre cuando los datos son validos`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(expenseGroupRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.updateGroup(1L, ExpenseGroupRequest("Nuevo nombre"), "alice")

        assertEquals("Nuevo nombre", response.name)
    }

    @Test
    fun `deleteGroup deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.deleteGroup(1L, "alice")
        }
    }

    @Test
    fun `deleteGroup deberia lanzar NotGroupCreatorException cuando el usuario no es el creador`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<NotGroupCreatorException> {
            service.deleteGroup(1L, "bob")
        }
    }

    @Test
    fun `deleteGroup deberia eliminar el grupo cuando el usuario es el creador`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        service.deleteGroup(1L, "alice")

        verify(expenseGroupRepository).delete(group)
    }

    @Test
    fun `addMember deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.addMember(1L, GroupMemberRequest("bob"), "alice")
        }
    }

    @Test
    fun `addMember deberia lanzar NotGroupMemberException cuando quien invita no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "carol")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.addMember(1L, GroupMemberRequest("bob"), "carol")
        }
    }

    @Test
    fun `addMember deberia lanzar MemberAlreadyExistsException cuando el usuario ya es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(true)

        assertThrows<MemberAlreadyExistsException> {
            service.addMember(1L, GroupMemberRequest("bob"), "alice")
        }
    }

    @Test
    fun `addMember deberia agregar el miembro cuando los datos son validos`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "alice")).thenReturn(true)
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)
        whenever(groupMemberRepository.save(any())).thenAnswer { invocation ->
            val member = invocation.arguments[0] as GroupMember
            member.id = 10L
            member
        }

        val response = service.addMember(1L, GroupMemberRequest("bob"), "alice")

        assertEquals("bob", response.memberUsername)
    }

    @Test
    fun `getMembers deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getMembers(1L, "alice")
        }
    }

    @Test
    fun `getMembers deberia lanzar NotGroupMemberException cuando el usuario no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getMembers(1L, "bob")
        }
    }

    @Test
    fun `getMembers deberia retornar la lista de miembros`() {
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
    fun `closeGroup deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.closeGroup(1L, "alice")
        }
    }

    @Test
    fun `closeGroup deberia lanzar NotGroupCreatorException cuando el usuario no es el creador`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThrows<NotGroupCreatorException> {
            service.closeGroup(1L, "bob")
        }
    }

    @Test
    fun `closeGroup deberia cerrar el grupo cuando el usuario es el creador`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(expenseGroupRepository.save(any())).thenAnswer { it.arguments[0] }

        val response = service.closeGroup(1L, "alice")

        assertEquals(true, response.closed)
    }

    @Test
    fun `getBalances deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getBalances(1L, "alice")
        }
    }

    @Test
    fun `getBalances deberia lanzar NotGroupMemberException cuando el usuario no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getBalances(1L, "bob")
        }
    }

    @Test
    fun `getBalances deberia calcular el saldo neto de cada usuario`() {
        val group = buildGroup()
        val expense = Expense(
            group = group,
            payerUsername = "alice",
            description = "Cena",
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
    fun `getNetting deberia lanzar ExpenseGroupNotFoundException cuando el grupo no existe`() {
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows<ExpenseGroupNotFoundException> {
            service.getNetting(1L, "alice")
        }
    }

    @Test
    fun `getNetting deberia lanzar NotGroupMemberException cuando el usuario no es miembro`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(groupMemberRepository.existsByGroupIdAndMemberUsername(1L, "bob")).thenReturn(false)

        assertThrows<NotGroupMemberException> {
            service.getNetting(1L, "bob")
        }
    }

    @Test
    fun `getNetting deberia calcular las transferencias minimas`() {
        val group = buildGroup()
        val expense = Expense(
            group = group,
            payerUsername = "alice",
            description = "Cena",
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
    fun `getInvitation deberia lanzar InvitationNotFoundException cuando el codigo no existe`() {
        whenever(expenseGroupRepository.findByInviteCode("XYZ")).thenReturn(null)

        assertThrows<InvitationNotFoundException> {
            service.getInvitation("XYZ")
        }
    }

    @Test
    fun `getInvitation deberia retornar los datos de la invitacion cuando el codigo existe`() {
        val group = buildGroup()
        whenever(expenseGroupRepository.findByInviteCode("ABC123")).thenReturn(group)
        whenever(groupMemberRepository.countByGroupId(1L)).thenReturn(3L)

        val response = service.getInvitation("ABC123")

        assertEquals("Viaje a la playa", response.groupName)
        assertEquals("alice", response.invitedBy)
        assertEquals(3L, response.memberCount)
    }
}
