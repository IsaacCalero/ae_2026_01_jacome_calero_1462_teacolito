package com.pucetec.teacolito.repositories

import com.pucetec.teacolito.entities.ExpenseGroup
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseGroupRepository : JpaRepository<ExpenseGroup, Long> {
    fun findByInviteCode(inviteCode: String): ExpenseGroup?
    fun existsByNameAndCreatedBy(name: String, createdBy: String): Boolean
    fun existsByNameAndCreatedByAndIdNot(name: String, createdBy: String, id: Long): Boolean
}
