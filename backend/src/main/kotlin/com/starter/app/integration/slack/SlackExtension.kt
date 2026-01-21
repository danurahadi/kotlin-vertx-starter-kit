package com.starter.app.integration.slack

import id.yoframework.core.extension.logger.LogType
import id.yoframework.core.extension.logger.log
import org.slf4j.Logger

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Logger.slackLog(log: LogType, vararg params: Pair<String, Any>) {
    val defaultParams = mapOf(
        "service_type" to "INTEGRATION",
        "service_target" to "SLACK_API"
    )
    this.log(log, defaultParam = defaultParams, params = params)
}
