package com.starter.app.domain.admin.plain

/**
 * DTO class for hold some admin stats data that will be attached to admin profile
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AdminStat(
    val id: String?,
    val anyUnreadChats: Boolean
)
