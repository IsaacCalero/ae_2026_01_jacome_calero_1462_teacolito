package com.pucetec.teacolito.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "expense_groups",
    uniqueConstraints = [UniqueConstraint(name = "uq_expense_groups_name_created_by", columnNames = ["name", "created_by"])]
)
class ExpenseGroup(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var inviteCode: String,

    @Column(nullable = false)
    var createdBy: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime,

    @Column(nullable = false)
    var closed: Boolean = false
)
