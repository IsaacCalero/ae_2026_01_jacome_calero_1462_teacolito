package com.pucetec.teacolito.mappers

import com.pucetec.teacolito.dto.GroupMemberResponse
import com.pucetec.teacolito.entities.GroupMember

fun GroupMember.toResponse(): GroupMemberResponse = GroupMemberResponse(
    id = id,
    groupId = group.id,
    memberUsername = memberUsername,
    joinedAt = joinedAt
)
