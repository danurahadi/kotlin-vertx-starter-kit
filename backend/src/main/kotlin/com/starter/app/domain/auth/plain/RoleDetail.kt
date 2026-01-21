package com.starter.app.domain.auth.plain

import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of get role details API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class RoleDetail(
    val id: String,
    val name: String,
    val alias: String,
    val description: String?,
    val createdAt: String,
    val lastUpdatedAt: String,
    val createdBy: LogActor?,
    val lastUpdatedBy: LogActor?
)
