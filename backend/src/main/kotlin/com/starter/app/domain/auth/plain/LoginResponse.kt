package com.starter.app.domain.auth.plain

import com.starter.app.domain.user.plain.UserResponse

/**
 * DTO class for wire response of login API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class LoginResponse(
    val account: UserResponse,
    val accessToken: String,
    val permissions: List<String>
)
