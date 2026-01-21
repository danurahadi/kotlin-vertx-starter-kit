package com.starter.app.domain.setting.db.model.value

import io.ebean.annotation.EnumValue

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

enum class BannerType {
    @EnumValue(value = "LEFT_BANNER")
    LEFT_BANNER,

    @EnumValue(value = "RIGHT_BANNER")
    RIGHT_BANNER,

    @EnumValue(value = "HOMEPAGE_SLIDESHOW")
    HOMEPAGE_SLIDESHOW
}
