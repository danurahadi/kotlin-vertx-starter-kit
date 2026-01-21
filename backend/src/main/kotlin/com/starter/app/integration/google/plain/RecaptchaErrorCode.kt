package com.starter.app.integration.google.plain

/**
 * DTO class for parse Google Recaptcha Verify API response error codes
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class RecaptchaErrorCode(
    val key: String,
    val value: List<String>
)
