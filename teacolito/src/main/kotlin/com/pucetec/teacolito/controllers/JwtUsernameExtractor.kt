package com.pucetec.teacolito.controllers

import org.springframework.security.oauth2.jwt.Jwt

fun Jwt.extractUsername(): String =
    this.getClaimAsString("username")
        ?: this.getClaimAsString("cognito:username")
        ?: this.subject
