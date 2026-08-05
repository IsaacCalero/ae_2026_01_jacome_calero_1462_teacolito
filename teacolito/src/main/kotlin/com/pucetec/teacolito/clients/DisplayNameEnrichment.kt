package com.pucetec.teacolito.clients

import com.pucetec.teacolito.dto.BalanceResponse
import com.pucetec.teacolito.dto.ExpenseGroupResponse
import com.pucetec.teacolito.dto.ExpenseResponse
import com.pucetec.teacolito.dto.ExpenseShareResponse
import com.pucetec.teacolito.dto.GroupMemberResponse
import com.pucetec.teacolito.dto.InvitationResponse
import com.pucetec.teacolito.dto.SettlementResponse
import com.pucetec.teacolito.dto.TransferResponse

fun ExpenseGroupResponse.enrich(client: UserClient, token: String): ExpenseGroupResponse =
    copy(createdByDisplayName = client.resolveDisplayName(createdBy, token))

fun GroupMemberResponse.enrich(client: UserClient, token: String): GroupMemberResponse =
    copy(displayName = client.resolveDisplayName(memberUsername, token))

fun List<GroupMemberResponse>.enrichMembers(client: UserClient, token: String): List<GroupMemberResponse> =
    map { it.enrich(client, token) }

fun BalanceResponse.enrich(client: UserClient, token: String): BalanceResponse =
    copy(displayName = client.resolveDisplayName(username, token))

fun List<BalanceResponse>.enrichBalances(client: UserClient, token: String): List<BalanceResponse> =
    map { it.enrich(client, token) }

fun TransferResponse.enrich(client: UserClient, token: String): TransferResponse =
    copy(
        fromDisplayName = client.resolveDisplayName(fromUsername, token),
        toDisplayName = client.resolveDisplayName(toUsername, token)
    )

fun List<TransferResponse>.enrichTransfers(client: UserClient, token: String): List<TransferResponse> =
    map { it.enrich(client, token) }

fun ExpenseShareResponse.enrich(client: UserClient, token: String): ExpenseShareResponse =
    copy(debtorDisplayName = client.resolveDisplayName(debtorUsername, token))

fun ExpenseResponse.enrich(client: UserClient, token: String): ExpenseResponse =
    copy(
        payerDisplayName = client.resolveDisplayName(payerUsername, token),
        shares = shares.map { it.enrich(client, token) }
    )

fun List<ExpenseResponse>.enrichExpenses(client: UserClient, token: String): List<ExpenseResponse> =
    map { it.enrich(client, token) }

fun SettlementResponse.enrich(client: UserClient, token: String): SettlementResponse =
    copy(
        fromDisplayName = client.resolveDisplayName(fromUsername, token),
        toDisplayName = client.resolveDisplayName(toUsername, token)
    )

fun List<SettlementResponse>.enrichSettlements(client: UserClient, token: String): List<SettlementResponse> =
    map { it.enrich(client, token) }

fun InvitationResponse.enrich(client: UserClient, token: String): InvitationResponse =
    copy(invitedByDisplayName = client.resolveDisplayName(invitedBy, token))
