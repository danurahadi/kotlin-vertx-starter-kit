package com.starter.app.domain.setting.plain

/**
 * DTO class for wire response of GET app setting list API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AppSettingList(
    val id: String,
    val settingGroupName: String,
    val settingKey: String,
    val settingValue: String,
    val createdAt: String,
    val lastUpdatedAt: String
)
