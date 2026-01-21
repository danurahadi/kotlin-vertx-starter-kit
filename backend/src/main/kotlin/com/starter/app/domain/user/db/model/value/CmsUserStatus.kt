package com.starter.app.domain.user.db.model.value

import io.ebean.annotation.EnumValue

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

enum class CmsUserStatus {
    @EnumValue(value = "PENDING")
    PENDING,

    @EnumValue(value = "ACTIVE")
    ACTIVE,

    @EnumValue(value = "SUSPENDED")
    SUSPENDED
}
