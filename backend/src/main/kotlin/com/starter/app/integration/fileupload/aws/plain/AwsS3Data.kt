package com.starter.app.integration.fileupload.aws.plain

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AwsS3Data (
    val originalFileName: String,
    val thumbnailFileName: String?,
    val originalLink: String,
    val thumbnailLink: String?,
    val resizeFailed: Boolean
)
