package com.pucetec.teacolito.repositories

import com.pucetec.teacolito.entities.ExpenseShare
import org.springframework.data.jpa.repository.JpaRepository

interface ExpenseShareRepository : JpaRepository<ExpenseShare, Long> {
    fun findByExpenseId(expenseId: Long): List<ExpenseShare>
}
