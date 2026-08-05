package com.pucetec.teacolito.repositories

import com.pucetec.teacolito.entities.Settlement
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByGroupId(groupId: Long): List<Settlement>
}
