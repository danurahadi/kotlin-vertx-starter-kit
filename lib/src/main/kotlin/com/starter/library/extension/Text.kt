package com.starter.library.extension

import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.string.slugify
import id.yoframework.extra.snowflake.nextAlpha
import java.net.URLEncoder
import java.util.*
import kotlin.math.abs

/**
 * [Documentation Here]
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

private val log = logger("Text_Manipulation_Extension")

fun String.urlEncodeId(): String {
    val slug = this
            .replace(" ", "-")
            .trim()

    return URLEncoder.encode(slug, "utf-8")
}

fun String.setupRole(): String {
    return this.uppercase()
        .replace(" ", "-")
        .replace("[^A-Z-]".toRegex(), "")
        .replace("\\s+".toRegex(), "")
}

fun String.setupSlug(): String {
    return this.lowercase()
        .replace(" ", "-")
        .replace("[^a-z0-9-]".toRegex(), "")
        .replace("\\s+".toRegex(), "")
}

fun String.setupUsername(): String {
    return this.lowercase()
        .replace(" ", "_")
        .replace("[^a-z0-9_]".toRegex(), "")
        .replace("\\s+".toRegex(), "")
}

fun String.setupUniqueSlug(): String {
    val slugId = abs(nextAlpha(17).hashCode())
    return "${this.slugify()}-$slugId"
}

fun String.removeQuotes(): String {
    return this.replace("['\"]".toRegex(), "")
}

fun String.setupUsernameFromEmail(): String {
    return this.lowercase()
        .replaceAfter("@", "")
        .replace("[^a-z0-9_]".toRegex(), "")
        .replace("\\s+".toRegex(), "")
}

fun String.sanitizeFileName(): String {
    return this.lowercase()
        .replace(" ", "-")
        .replace("[^a-z0-9._-]".toRegex(), "")
        .replace("\\s+".toRegex(), "")
}

fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(Locale.getDefault())
        } else {
            it.toString()
        }
    }
}

fun String.decapitalize(): String {
    return this.replaceFirstChar { it.lowercase(Locale.getDefault()) }
}

fun String.containsInArray(terms: Array<String>): Boolean {
    return terms.any { term -> this.contains(term) }
}

