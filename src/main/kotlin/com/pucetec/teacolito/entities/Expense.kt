package com.pucetec.teacolito.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "expenses")
class Expense(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    var group: ExpenseGroup,

    @Column(nullable = false)
    var payerUsername: String,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false)
    var amount: BigDecimal,

    @Column(nullable = false)
    var spentAt: LocalDateTime
)
