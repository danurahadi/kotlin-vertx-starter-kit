package com.starter.app.domain.setting.plain

/**
 * DTO class for wire response of GET all app settings API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class SettingGroupCompact(
    val id: String,
    val name: String,
    val appSettings: List<AppSettingCompact>
)
