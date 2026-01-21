package com.starter.app.domain.user.plain

/**
 * DTO class for hold user data that create or update some data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class LogActor(
    val id: String,
    val username: String,
    val fullName: String?,
    val thumbnailProfileImage: String?
)
