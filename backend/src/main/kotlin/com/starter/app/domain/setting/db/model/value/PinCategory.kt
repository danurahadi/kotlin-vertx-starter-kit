package com.starter.app.domain.setting.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the pin setting category
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

enum class PinCategory {
    @EnumValue(value = "DAILY_CLOSING")
    DAILY_CLOSING,

    @EnumValue(value = "TRANSFER_BILL")
    TRANSFER_BILL
}
