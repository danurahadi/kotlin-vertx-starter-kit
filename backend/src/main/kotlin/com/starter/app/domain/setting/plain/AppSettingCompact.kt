package com.starter.app.domain.setting.plain

import com.starter.app.domain.setting.db.model.value.SettingType
import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for hold app setting data that attached to the setting groups data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AppSettingCompact(
    val id: String,
    val settingKey: String,
    val settingValue: String,
    val settingType: SettingType,
    val settingOptions: List<String>?,
    val lastUpdatedAt: String,
    val lastUpdatedBy: LogActor?
)
