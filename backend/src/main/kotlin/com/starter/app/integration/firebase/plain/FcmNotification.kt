package com.starter.app.integration.firebase.plain

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class FcmNotification (
    val title: String,
    val body: String,
    val icon: String,
    val sound: String
)
