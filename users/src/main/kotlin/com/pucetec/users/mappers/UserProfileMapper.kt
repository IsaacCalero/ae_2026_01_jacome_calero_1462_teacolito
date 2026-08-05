package com.pucetec.users.mappers

import com.pucetec.users.dto.UserProfileResponse
import com.pucetec.users.entities.UserProfile

fun UserProfile.toResponse(): UserProfileResponse = UserProfileResponse(
    username = sub,
    displayName = displayName
)
