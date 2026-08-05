package com.pucetec.users.repositories

import com.pucetec.users.entities.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findBySub(sub: String): UserProfile?
    fun existsBySub(sub: String): Boolean

    @Query("SELECT COUNT(p) > 0 FROM UserProfile p WHERE LOWER(p.displayName) = LOWER(:displayName)")
    fun existsByDisplayName(@Param("displayName") displayName: String): Boolean
}
