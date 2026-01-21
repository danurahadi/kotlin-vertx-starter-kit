package com.starter.app.domain.auth.plain

/**
 * DTO class for wire response of get role list API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class RoleList(
    val id: String,
    val name: String,
    val alias: String,
    val createdAt: String,
    val lastUpdatedAt: String
)
