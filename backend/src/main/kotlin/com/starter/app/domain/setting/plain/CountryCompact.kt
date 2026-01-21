package com.starter.app.domain.setting.plain

/**
 * DTO class for wire response of GET all countries API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class CountryCompact(
    val id: String,
    val name: String,
    val alpha2Code: String?,
    val callingCode: String?
)
