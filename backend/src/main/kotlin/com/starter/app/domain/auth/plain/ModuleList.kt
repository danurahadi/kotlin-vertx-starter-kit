package com.starter.app.domain.auth.plain

/**
 * DTO class for wire response of GET module list API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ModuleList(
    val id: String,
    val code: String,
    val name: String,
    val createdAt: String,
    val lastUpdatedAt: String
)
