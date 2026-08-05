package com.pucetec.teacolito.repositories

import com.pucetec.teacolito.entities.GroupMember
import org.springframework.data.jpa.repository.JpaRepository

interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    fun existsByGroupIdAndMemberUsername(groupId: Long, memberUsername: String): Boolean
    fun findByGroupId(groupId: Long): List<GroupMember>
    fun countByGroupId(groupId: Long): Long
}
