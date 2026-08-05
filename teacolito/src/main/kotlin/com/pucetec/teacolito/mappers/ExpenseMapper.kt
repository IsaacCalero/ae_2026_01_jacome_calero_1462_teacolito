package com.pucetec.teacolito.mappers

import com.pucetec.teacolito.dto.ExpenseResponse
import com.pucetec.teacolito.dto.ExpenseShareResponse
import com.pucetec.teacolito.entities.Expense
import com.pucetec.teacolito.entities.ExpenseShare

fun ExpenseShare.toResponse(): ExpenseShareResponse = ExpenseShareResponse(
    id = id,
    debtorUsername = debtorUsername,
    shareAmount = shareAmount
)

fun Expense.toResponse(shares: List<ExpenseShare>): ExpenseResponse = ExpenseResponse(
    id = id,
    groupId = group.id,
    payerUsername = payerUsername,
    description = description,
    amount = amount,
    spentAt = spentAt,
    shares = shares.map { it.toResponse() }
)
