package com.pucetec.teacolito.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ExpenseShareRequest(
    val debtorUsername: String,
    val shareAmount: BigDecimal
)

data class ExpenseShareResponse(
    val id: Long,
    val debtorUsername: String,
    val debtorDisplayName: String = "",
    val shareAmount: BigDecimal
)

data class ExpenseRequest(
    val groupId: Long,
    val description: String,
    val amount: BigDecimal,
    val shares: List<ExpenseShareRequest>
)

data class ExpenseResponse(
    val id: Long,
    val groupId: Long,
    val payerUsername: String,
    val payerDisplayName: String = "",
    val description: String,
    val amount: BigDecimal,
    val spentAt: LocalDateTime,
    val shares: List<ExpenseShareResponse>
)
