package com.starter.app.domain.admin.plain

import com.starter.app.domain.user.db.model.value.Gender
import java.time.LocalDate

/**
 * DTO class for wire response of GET admin details API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class AdminDetail(
    val id: String,
    val fullName: String,
    val thumbnailProfileImage: String?,
    val originalProfileImage: String?,
    val gender: Gender?,
    val birthday: LocalDate?,
    val complete: Boolean,
    val superadmin: Boolean,
    val unreadNotifsCount: Int,
    val createdAt: String,
    val lastUpdatedAt: String
)
