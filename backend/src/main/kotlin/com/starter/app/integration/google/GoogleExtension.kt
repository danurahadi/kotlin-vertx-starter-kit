package com.starter.app.integration.google

import id.yoframework.core.extension.logger.LogType
import id.yoframework.core.extension.logger.log
import org.slf4j.Logger

/**
 * Extension functions for Google client-lib integration
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Logger.googleLog(log: LogType, vararg params: Pair<String, Any>) {
    val defaultParams = mapOf(
        "service_type" to "INTEGRATION",
        "service_target" to "GOOGLE_API"
    )
    this.log(log, defaultParam = defaultParams, params = params)
}
