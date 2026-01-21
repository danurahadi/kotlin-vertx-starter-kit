package com.starter.app.domain.setting.plain

import com.starter.app.domain.setting.db.model.value.SettingType
import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of GET app setting details API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AppSettingDetail(
    val id: String,
    val settingGroup: SettingGroupDetail,
    val settingKey: String,
    val settingValue: String,
    val settingType: SettingType,
    val settingOptions: List<String>?,
    val createdAt: String,
    val lastUpdatedAt: String,
    val createdBy: LogActor?,
    val lastUpdatedBy: LogActor?
)
