package com.starter.app.domain.user.plain

/**
 * DTO class for wire response of GET users list & detail API to the client (base class)
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class UserResponse(
    val user: Any?,
    val admin: Any?,
    val settings: UserSettings? = null
)
