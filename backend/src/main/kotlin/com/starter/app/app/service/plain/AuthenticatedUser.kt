package com.starter.app.app.service.plain

import com.starter.app.domain.user.db.model.CmsUser

/**
 * DTO class for hold authenticated User data with their allowed access list
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AuthenticatedUser(
    val user: CmsUser,
    val accessList: List<String>
)
