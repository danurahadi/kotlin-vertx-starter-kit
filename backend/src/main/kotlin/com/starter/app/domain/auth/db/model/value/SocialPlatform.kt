package com.starter.app.domain.auth.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the social login providers for social sign-in features
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

enum class SocialPlatform {
    @EnumValue(value = "GOOGLE")
    GOOGLE,

    @EnumValue(value = "FACEBOOK")
    FACEBOOK,

    @EnumValue(value = "TWITTER")
    TWITTER,

    @EnumValue(value = "APPLE")
    APPLE
}
