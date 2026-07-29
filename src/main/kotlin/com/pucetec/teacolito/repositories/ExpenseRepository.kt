package com.pucetec.teacolito.repositories

import com.pucetec.teacolito.entities.Expense
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findByGroupId(groupId: Long): List<Expense>
}
