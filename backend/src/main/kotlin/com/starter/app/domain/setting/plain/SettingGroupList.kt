package com.starter.app.domain.setting.plain

/**
 * DTO class for wire response of GET setting group list API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class SettingGroupList(
    val id: String,
    val name: String,
    val createdAt: String,
    val lastUpdatedAt: String
)
