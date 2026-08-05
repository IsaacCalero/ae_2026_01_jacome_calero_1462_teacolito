package com.pucetec.users.controllers

import com.pucetec.users.dto.CreateUserProfileRequest
import com.pucetec.users.dto.UserProfileResponse
import com.pucetec.users.services.UserProfileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserProfileController(
    private val userProfileService: UserProfileService
) {

    @PostMapping("/users")
    fun createProfile(
        @RequestBody request: CreateUserProfileRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<UserProfileResponse> {
        val response = userProfileService.createProfile(request, jwt.subject)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/users/{username}")
    fun getProfile(@PathVariable username: String): ResponseEntity<UserProfileResponse> {
        val response = userProfileService.getProfile(username)
        return ResponseEntity.ok(response)
    }
}
