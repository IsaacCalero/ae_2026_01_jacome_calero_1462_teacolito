package com.pucetec.teacolito.repositories

import com.pucetec.teacolito.entities.ExpenseGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseGroupRepository : JpaRepository<ExpenseGroup, Long> {
    fun findByInviteCode(inviteCode: String): ExpenseGroup?

    @Query(
        "SELECT COUNT(g) > 0 FROM ExpenseGroup g " +
            "WHERE LOWER(g.name) = LOWER(:name) AND g.createdBy = :createdBy"
    )
    fun existsByNameAndCreatedBy(@Param("name") name: String, @Param("createdBy") createdBy: String): Boolean

    @Query(
        "SELECT COUNT(g) > 0 FROM ExpenseGroup g " +
            "WHERE LOWER(g.name) = LOWER(:name) AND g.createdBy = :createdBy AND g.id <> :id"
    )
    fun existsByNameAndCreatedByAndIdNot(
        @Param("name") name: String,
        @Param("createdBy") createdBy: String,
        @Param("id") id: Long
    ): Boolean
}
