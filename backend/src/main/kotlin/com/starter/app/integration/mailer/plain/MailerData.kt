package com.starter.app.integration.mailer.plain

import java.io.File

/**
 * [Documentation Here]
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class MailerData(
    val recipient: String?,
    val subject: String,
    val htmlContent: String,
    val sender: String? = null,
    val attachments: List<File> = emptyList()
)
