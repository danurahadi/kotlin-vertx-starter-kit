package com.starter.app.domain.notification.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the notification status
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

enum class NotificationStatus {
    @EnumValue(value = "NEW")
    NEW,

    @EnumValue(value = "READ")
    READ
}
