package com.pucetec.teacolito.dto

data class InvitationResponse(
    val groupName: String,
    val invitedBy: String,
    val invitedByDisplayName: String = "",
    val memberCount: Long
)
