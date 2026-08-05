package com.pucetec.teacolito.mappers

import com.pucetec.teacolito.dto.ExpenseGroupResponse
import com.pucetec.teacolito.entities.ExpenseGroup

fun ExpenseGroup.toResponse(): ExpenseGroupResponse = ExpenseGroupResponse(
    id = id,
    name = name,
    inviteCode = inviteCode,
    createdBy = createdBy,
    createdAt = createdAt,
    closed = closed
)
