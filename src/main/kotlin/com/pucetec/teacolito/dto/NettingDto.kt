package com.pucetec.teacolito.dto

import java.math.BigDecimal

data class TransferResponse(
    val fromUsername: String,
    val toUsername: String,
    val amount: BigDecimal
)
