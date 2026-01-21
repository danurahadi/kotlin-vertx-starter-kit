package com.starter.app.domain.setting.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the setting groups type
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

enum class SettingType {
    @EnumValue(value = "BOOLEAN")
    BOOLEAN,

    @EnumValue(value = "TEXT")
    TEXT,

    @EnumValue(value = "NUMERIC")
    NUMERIC,

    @EnumValue(value = "RADIO")
    RADIO,

    @EnumValue(value = "MULTIPLE")
    MULTIPLE
}
