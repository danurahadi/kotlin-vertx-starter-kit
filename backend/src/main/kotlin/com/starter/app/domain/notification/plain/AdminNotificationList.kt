package com.starter.app.domain.notification.plain

import com.starter.app.domain.notification.db.model.value.NotificationStatus
import com.starter.app.domain.user.plain.LogActor

/**
 * DTO class for wire response of get admin notification list API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AdminNotificationList(
    val id: String,
    val message: String,
    val status: NotificationStatus,
    val createdAt: String,
    val actor: LogActor?
)
