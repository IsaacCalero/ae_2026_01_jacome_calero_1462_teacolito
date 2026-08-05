package com.pucetec.users.services

import com.pucetec.users.dto.CreateUserProfileRequest
import com.pucetec.users.dto.UserProfileResponse
import com.pucetec.users.entities.UserProfile
import com.pucetec.users.exceptions.BlankFieldException
import com.pucetec.users.exceptions.DuplicateDisplayNameException
import com.pucetec.users.exceptions.UserProfileAlreadyExistsException
import com.pucetec.users.exceptions.UserProfileNotFoundException
import com.pucetec.users.mappers.toResponse
import com.pucetec.users.repositories.UserProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserProfileService(
    private val userProfileRepository: UserProfileRepository
) {

    private val log = LoggerFactory.getLogger(UserProfileService::class.java)

    fun createProfile(request: CreateUserProfileRequest, sub: String): UserProfileResponse {
        if (request.displayName.isBlank()) {
            log.warn("event=profile.create.rejected | msg=Blank display name | sub=$sub")
            throw BlankFieldException("Display name cannot be blank")
        }

        if (userProfileRepository.existsBySub(sub)) {
            log.warn("event=profile.create.rejected | msg=Profile already exists | sub=$sub")
            throw UserProfileAlreadyExistsException("A profile already exists for user $sub")
        }

        if (userProfileRepository.existsByDisplayName(request.displayName)) {
            log.warn("event=profile.create.rejected | msg=Display name already taken | displayName=\"${request.displayName}\"")
            throw DuplicateDisplayNameException("Display name '${request.displayName}' is already taken")
        }

        val profile = userProfileRepository.save(
            UserProfile(
                sub = sub,
                displayName = request.displayName,
                createdAt = LocalDateTime.now()
            )
        )

        log.info("event=profile.created | msg=Profile created | sub=$sub displayName=\"${request.displayName}\"")
        return profile.toResponse()
    }

    fun getProfile(sub: String): UserProfileResponse {
        val profile = userProfileRepository.findBySub(sub)
            ?: throw UserProfileNotFoundException("No profile exists for user $sub")
        return profile.toResponse()
    }
}
