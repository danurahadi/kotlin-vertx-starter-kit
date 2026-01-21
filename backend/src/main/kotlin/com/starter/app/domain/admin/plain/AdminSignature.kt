package com.starter.app.domain.admin.plain

/**
 * DTO class for hold basic & signature-related admin data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AdminSignature(
    val id: String,
    val email: String,
    val username: String,
    val fullName: String,
    val position: String?,
    val thumbnailProfileImage: String?,
    val signatureBase64Image: String?
)
