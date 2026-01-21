package com.starter.app.integration.fileupload

import id.yoframework.core.extension.logger.LogType
import id.yoframework.core.extension.logger.log
import org.slf4j.Logger

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Logger.fileUploadLog(log: LogType, vararg params: Pair<String, Any>) {
    val defaultParams = mapOf(
        "service_type" to "INTEGRATION",
        "service_target" to "FILEUPLOAD"
    )
    this.log(log, defaultParam = defaultParams, params = params)
}

typealias FileName = String
typealias FilePath = String
