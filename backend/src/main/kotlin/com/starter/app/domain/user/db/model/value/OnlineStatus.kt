package com.starter.app.domain.user.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the user's online status.
 * The status will be implemented on real-time updates using Vert.x EventBus in the near future.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

enum class OnlineStatus {
    @EnumValue(value = "ACTIVE")
    ACTIVE,

    @EnumValue(value = "AWAY")
    AWAY
}
