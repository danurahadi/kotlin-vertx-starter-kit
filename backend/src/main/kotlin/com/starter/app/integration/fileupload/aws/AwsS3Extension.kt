package com.starter.app.integration.fileupload.aws

import id.yoframework.core.extension.logger.LogType
import id.yoframework.core.extension.logger.log
import org.slf4j.Logger

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun Logger.awsS3Log(log: LogType, vararg params: Pair<String, Any>) {
    val defaultParams = mapOf(
        "service_type" to "INTEGRATION",
        "service_target" to "FILEUPLOAD_AWS_S3"
    )
    this.log(log, defaultParam = defaultParams, params = params)
}

typealias AwsS3Filename = String
typealias AwsS3MediaLink = String
typealias DataId = Long
