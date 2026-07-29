package com.pucetec.teacolito.dto

data class InvitationResponse(
    val groupName: String,
    val invitedBy: String,
    val memberCount: Long
)
