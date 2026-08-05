package com.pucetec.teacolito.dto

import java.time.LocalDateTime

data class JoinGroupRequest(
    val code: String
)

data class GroupMemberResponse(
    val id: Long,
    val groupId: Long,
    val memberUsername: String,
    val displayName: String = "",
    val joinedAt: LocalDateTime
)
