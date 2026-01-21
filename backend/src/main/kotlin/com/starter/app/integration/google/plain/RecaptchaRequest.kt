package com.starter.app.integration.google.plain

/**
 * DTO class for hold the Google Recaptcha Verify API request data
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class RecaptchaRequest(
    val secret: String,
    val response: String,
    val remoteIp: String?
)
