package com.starter.app.domain.user

import id.yoframework.core.extension.logger.LogType
import id.yoframework.core.extension.logger.log
import org.slf4j.Logger

/**
 * Class that containing all extension functions for user module
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Logger.userLog(log: LogType, vararg params: Pair<String, Any>) {
    val defaultParams = mapOf(
        "service_type" to "DOMAIN",
        "service_target" to "USER"
    )
    this.log(log, defaultParam = defaultParams, params = params)
}
