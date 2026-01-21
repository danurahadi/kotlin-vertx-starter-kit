package com.starter.app.domain.auth.plain

/**
 * DTO class for wire response of get API key list API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ApiKeyList(
    val id: String,
    val title: String,
    val accessToken: String,
    val createdAt: String,
    val expiredAt: String?
)
