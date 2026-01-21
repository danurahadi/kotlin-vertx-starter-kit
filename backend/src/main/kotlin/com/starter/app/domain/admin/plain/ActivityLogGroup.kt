package com.starter.app.domain.admin.plain

/**
 * DTO class for grouping activity logs by date
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ActivityLogGroup(
    val date: String,
    val logs: List<ActivityLogList>
)
