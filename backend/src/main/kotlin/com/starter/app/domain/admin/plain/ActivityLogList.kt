package com.starter.app.domain.admin.plain

import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of get activity logs API
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ActivityLogList(
    val id: String,
    val description: String,
    val createdAt: String,
    val actor: LogActor?
)
