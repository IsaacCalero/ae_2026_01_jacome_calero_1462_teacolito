package com.pucetec.teacolito.dto

import java.math.BigDecimal

data class TransferResponse(
    val fromUsername: String,
    val fromDisplayName: String = "",
    val toUsername: String,
    val toDisplayName: String = "",
    val amount: BigDecimal
)
