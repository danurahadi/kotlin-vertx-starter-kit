package com.starter.library.extension

import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.web.exception.orDataError
import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom
import java.util.*
import java.util.random.RandomGenerator
import kotlin.math.absoluteValue

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun generatePassword(): String {
    val alpha = RandomStringUtils.random(
        4, 0, 0,
        true, false, null,
        Random.from(RandomGenerator.getDefault())
    ).lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
    val numeric = RandomStringUtils.random(
        4, 0, 0,
        false, true, null,
        Random.from(RandomGenerator.getDefault())
    )
    return "$alpha$numeric"
}

fun generateRandomNumber(algorithm: String): String {
    return SecureRandom.getInstance(algorithm).nextInt().absoluteValue.toString()
}

fun generateUniqueCode(): String {
    val uuid = UUID.randomUUID().toString().split("-")
    val firstPart = uuid.firstOrNull() orDataError "Invalid UUID."
    return firstPart.uppercase()
}

fun generateUUID(): String {
    return UUID.randomUUID().toString()
}

fun generateUUIDMostSigBits(digits: Int? = null): String {
    if (digits != null && digits > 18) throw DataInconsistentException("Invalid digits number.")

    return if (digits == null) {
        UUID.randomUUID().mostSignificantBits.absoluteValue.toString()
    } else {
        UUID.randomUUID().mostSignificantBits.absoluteValue.toString().substring(startIndex = 0, endIndex = digits)
    }
}

fun generateUUIDLeastSigBits(digits: Int? = null): String {
    if (digits != null && digits > 18) throw DataInconsistentException("Invalid digits number.")

    return if (digits == null) {
        UUID.randomUUID().leastSignificantBits.absoluteValue.toString()
    } else {
        UUID.randomUUID().leastSignificantBits.absoluteValue.toString().substring(startIndex = 0, endIndex = digits)
    }
}

fun generateSecureRandomChars(size: Int): String {
    val random = SecureRandom()
    val keyBytes = ByteArray(size)
    random.nextBytes(keyBytes)
    return keyBytes.joinToString("") { "%02x".format(it) }
}
