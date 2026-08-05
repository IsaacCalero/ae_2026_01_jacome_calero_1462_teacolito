package com.pucetec.teacolito.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class SettlementRequest(
    val groupId: Long,
    val toUsername: String,
    val amount: BigDecimal
)

data class SettlementResponse(
    val id: Long,
    val groupId: Long,
    val fromUsername: String,
    val fromDisplayName: String = "",
    val toUsername: String,
    val toDisplayName: String = "",
    val amount: BigDecimal,
    val settledAt: LocalDateTime
)
