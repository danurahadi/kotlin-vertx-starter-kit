package com.starter.app.domain.admin.plain

/**
 * DTO class for wire response of GET admin list API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class AdminList(
    val id: String,
    val fullName: String,
    val thumbnailProfileImage: String?,
    val complete: Boolean,
    val superadmin: Boolean,
    val createdAt: String,
    val lastUpdatedAt: String
)
