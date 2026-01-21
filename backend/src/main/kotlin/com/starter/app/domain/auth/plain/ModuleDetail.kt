package com.starter.app.domain.auth.plain

import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of GET module details API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class ModuleDetail(
    val id: String,
    val code: String,
    val name: String,
    val summary: String,
    val createdAt: String,
    val lastUpdatedAt: String,
    val createdBy: LogActor?,
    val lastUpdatedBy: LogActor?
)
