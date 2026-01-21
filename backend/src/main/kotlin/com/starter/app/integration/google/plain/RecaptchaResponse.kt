package com.starter.app.integration.google.plain

/**
 * DTO class for parse Google Recaptcha Verify API response
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class RecaptchaResponse(
    val success: Boolean,
    val challengeTs: String?,
    val hostname: String?,
    val score: Double?,
    val errorCodes: List<String>?
)
