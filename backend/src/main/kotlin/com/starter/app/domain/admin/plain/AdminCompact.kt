package com.starter.app.domain.admin.plain

/**
 * DTO class for wire response of get team admin autocomplete by team
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AdminCompact(
    val id: String,
    val username: String,
    val fullName: String,
    val thumbnailProfileImage: String?
)
