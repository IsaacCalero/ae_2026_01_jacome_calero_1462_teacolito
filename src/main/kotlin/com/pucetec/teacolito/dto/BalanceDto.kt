package com.pucetec.teacolito.dto

import java.math.BigDecimal

data class BalanceResponse(
    val username: String,
    val netAmount: BigDecimal
)
