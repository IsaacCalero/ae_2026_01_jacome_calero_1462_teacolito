package com.pucetec.users.services

import com.pucetec.users.dto.CreateUserProfileRequest
import com.pucetec.users.entities.UserProfile
import com.pucetec.users.exceptions.BlankFieldException
import com.pucetec.users.exceptions.DuplicateDisplayNameException
import com.pucetec.users.exceptions.UserProfileAlreadyExistsException
import com.pucetec.users.exceptions.UserProfileNotFoundException
import com.pucetec.users.repositories.UserProfileRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class UserProfileServiceTest {

    @Mock
    lateinit var userProfileRepository: UserProfileRepository

    @InjectMocks
    lateinit var service: UserProfileService

    @Test
    fun `createProfile should throw BlankFieldException when display name is blank`() {
        assertThrows<BlankFieldException> {
            service.createProfile(CreateUserProfileRequest("   "), "sub-1")
        }
    }

    @Test
    fun `createProfile should throw UserProfileAlreadyExistsException when sub already has a profile`() {
        whenever(userProfileRepository.existsBySub("sub-1")).thenReturn(true)

        assertThrows<UserProfileAlreadyExistsException> {
            service.createProfile(CreateUserProfileRequest("alice"), "sub-1")
        }
    }

    @Test
    fun `createProfile should throw DuplicateDisplayNameException when the name is already taken`() {
        whenever(userProfileRepository.existsBySub("sub-1")).thenReturn(false)
        whenever(userProfileRepository.existsByDisplayName("alice")).thenReturn(true)

        assertThrows<DuplicateDisplayNameException> {
            service.createProfile(CreateUserProfileRequest("alice"), "sub-1")
        }
    }

    @Test
    fun `createProfile should create the profile when data is valid`() {
        whenever(userProfileRepository.existsBySub("sub-1")).thenReturn(false)
        whenever(userProfileRepository.existsByDisplayName("alice")).thenReturn(false)
        whenever(userProfileRepository.save(any())).thenAnswer { invocation ->
            val profile = invocation.arguments[0] as UserProfile
            profile.id = 1L
            profile
        }

        val response = service.createProfile(CreateUserProfileRequest("alice"), "sub-1")

        assertEquals("sub-1", response.username)
        assertEquals("alice", response.displayName)
    }

    @Test
    fun `getProfile should throw UserProfileNotFoundException when no profile exists`() {
        whenever(userProfileRepository.findBySub("sub-1")).thenReturn(null)

        assertThrows<UserProfileNotFoundException> {
            service.getProfile("sub-1")
        }
    }

    @Test
    fun `getProfile should return the display name when the profile exists`() {
        val profile = UserProfile(sub = "sub-1", displayName = "alice", createdAt = LocalDateTime.now())
        profile.id = 1L
        whenever(userProfileRepository.findBySub("sub-1")).thenReturn(profile)

        val response = service.getProfile("sub-1")

        assertEquals("sub-1", response.username)
        assertEquals("alice", response.displayName)
    }
}
