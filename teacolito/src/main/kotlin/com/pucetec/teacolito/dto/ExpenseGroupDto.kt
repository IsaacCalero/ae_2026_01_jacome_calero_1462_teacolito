package com.pucetec.teacolito.dto

import java.time.LocalDateTime

data class ExpenseGroupRequest(
    val name: String
)

data class ExpenseGroupResponse(
    val id: Long,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val createdByDisplayName: String = "",
    val createdAt: LocalDateTime,
    val closed: Boolean
)
