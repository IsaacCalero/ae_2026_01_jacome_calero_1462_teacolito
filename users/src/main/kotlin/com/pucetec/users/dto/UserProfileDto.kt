package com.pucetec.users.dto

data class CreateUserProfileRequest(
    val displayName: String
)

data class UserProfileResponse(
    val username: String,
    val displayName: String
)
