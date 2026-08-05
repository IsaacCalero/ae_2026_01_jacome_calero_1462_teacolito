package com.pucetec.users.repositories

import com.pucetec.users.entities.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findBySub(sub: String): UserProfile?
    fun existsBySub(sub: String): Boolean
    fun existsByDisplayName(displayName: String): Boolean
}
