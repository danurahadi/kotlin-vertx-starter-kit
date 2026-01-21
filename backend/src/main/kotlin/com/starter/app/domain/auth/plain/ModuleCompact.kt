package com.starter.app.domain.auth.plain

/**
 * DTO class for wire response of GET all modules API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ModuleCompact(
    val id: String,
    val code: String,
    val name: String
)
