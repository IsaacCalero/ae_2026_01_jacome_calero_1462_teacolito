package com.pucetec.teacolito.dto

import java.time.LocalDateTime

data class GroupMemberRequest(
    val memberUsername: String
)

data class GroupMemberResponse(
    val id: Long,
    val groupId: Long,
    val memberUsername: String,
    val joinedAt: LocalDateTime
)
