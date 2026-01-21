package com.starter.app.domain.user.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the user's gender
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

enum class Gender {
    @EnumValue(value = "MALE")
    MALE,

    @EnumValue(value = "FEMALE")
    FEMALE,

    @EnumValue(value = "UNDEFINED")
    UNDEFINED
}
