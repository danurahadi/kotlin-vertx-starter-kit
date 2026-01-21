package com.starter.app.domain.notification.plain

/**
 * DTO class for hold unread notifications count that will be sent as Event Bus message
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class UnreadNotification(
    val id: String,
    val unreadCount: Int
)
