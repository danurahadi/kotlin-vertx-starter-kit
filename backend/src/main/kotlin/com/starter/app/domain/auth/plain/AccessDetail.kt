package com.starter.app.domain.auth.plain

import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of get access details API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AccessDetail(
    val id: String,
    val module: ModuleDetail?,
    val name: String,
    val alias: String,
    val createdAt: String,
    val lastUpdatedAt: String,
    val createdBy: LogActor?,
    val lastUpdatedBy: LogActor?
)
