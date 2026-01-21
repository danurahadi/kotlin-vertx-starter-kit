package com.starter.app.domain.auth.plain

/**
 * DTO class for wire response of GET access list by role ID API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ModuleAccessList(
    val id: String,
    val code: String,
    val name: String,
    val accessList: List<AccessList>,
    val createdAt: String,
    val lastUpdatedAt: String
)
