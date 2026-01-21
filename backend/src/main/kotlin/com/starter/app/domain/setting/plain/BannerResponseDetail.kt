package com.starter.app.domain.setting.plain

import id.yoframework.core.model.Model

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class BannerResponseDetail(
    val id: String?,
    val title: String,
    val description: String?,
    val linkUrl: String?,
    val image: String?,
    val type: String,
    val sequence: Int,
    val status: Boolean,
    val createdAt: String?,
    val updatedAt: String?
): Model
