package com.starter.app.domain.setting.plain

import com.starter.app.domain.setting.db.model.value.BannerType
import id.yoframework.core.model.Model

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class BannerRequest(
    var title: String?,
    var description: String?,
    var linkUrl: String,
    var type: String = BannerType.LEFT_BANNER.toString(),
    var base64String: String,
    var fileName: String,
    var status: Boolean = true
): Model
