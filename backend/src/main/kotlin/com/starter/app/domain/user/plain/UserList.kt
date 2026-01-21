package com.starter.app.domain.user.plain

import com.starter.app.domain.user.db.model.value.CmsUserStatus

/**
 * DTO class for wire response of GET user list API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class UserList(
    val id: String,
    val email: String,
    val username: String,
    val phone: String?,
    val emailVerified: Boolean,
    val roleName: String,
    val locked: Boolean,
    val status: CmsUserStatus,
    val createdAt: String,
    val lastUpdatedAt: String,
    val createdBy: LogActor?,
    val lastUpdatedBy: LogActor?
)
