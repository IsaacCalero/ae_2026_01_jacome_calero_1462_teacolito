package com.pucetec.teacolito.mappers

import com.pucetec.teacolito.dto.SettlementResponse
import com.pucetec.teacolito.entities.Settlement

fun Settlement.toResponse(): SettlementResponse = SettlementResponse(
    id = id,
    groupId = group.id,
    fromUsername = fromUsername,
    toUsername = toUsername,
    amount = amount,
    settledAt = settledAt
)
