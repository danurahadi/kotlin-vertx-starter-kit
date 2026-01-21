package com.starter.app.scheduler

import id.yoframework.core.extension.logger.LogType
import id.yoframework.core.extension.logger.log
import org.slf4j.Logger

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Logger.schedulerLog(log: LogType, vararg params: Pair<String, Any>) {
    val defaultParams = mapOf(
        "service_type" to "SCHEDULER",
        "service_target" to "TASK_SCHEDULER"
    )
    this.log(log, defaultParam = defaultParams, params = params)
}
