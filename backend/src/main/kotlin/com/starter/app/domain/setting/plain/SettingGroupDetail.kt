package com.starter.app.domain.setting.plain

import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of GET setting group details API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class SettingGroupDetail(
    val id: String,
    val name: String,
    val createdAt: String,
    val lastUpdatedAt: String,
    val createdBy: LogActor?,
    val lastUpdatedBy: LogActor?
)
