package com.starter.app.integration.fileupload.aws.plain

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AwsS3Migration(
    val dataId: Long?,
    val filePath: String,
    val fileName: String,
    val folderName: String,
    val contentType: String
)
