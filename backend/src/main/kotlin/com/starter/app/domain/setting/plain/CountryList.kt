package com.starter.app.domain.setting.plain

/**
 * DTO class for wire response of GET country list API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class CountryList(
    val id: String,
    val alpha2Code: String?,
    val name: String,
    val callingCode: String?,
    val createdAt: String,
    val lastUpdatedAt: String
)
