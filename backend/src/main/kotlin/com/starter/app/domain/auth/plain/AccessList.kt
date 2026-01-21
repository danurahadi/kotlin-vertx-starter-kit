package com.starter.app.domain.auth.plain

/**
 * DTO class for wire response of get access list API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class AccessList(
    val id: String,
    val moduleName: String?,
    val name: String,
    val alias: String,
    val createdAt: String,
    val lastUpdatedAt: String
)
