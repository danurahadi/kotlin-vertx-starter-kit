package com.starter.library.extension

import id.yoframework.extra.extension.regex.matchWith

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

fun String.matchWithEmail(): Boolean {
    return this.matchWith("^((\"[\\w-\\s]+\")|([\\w-]+(?:\\.[\\w-]+)*)|(\"[\\w-\\s]+\")([\\w-]+" +
            "(?:\\.[\\w-]+)*))(@((?:[\\w-]+\\.)*\\w[\\w-]{0,66})\\.([a-z]{2,6}(?:\\.[a-z]{2})?)\$)|" +
            "(@\\[?((25[0-5]\\.|2[0-4][0-9]\\.|1[0-9]{2}\\.|[0-9]{1,2}\\.))((25[0-5]|2[0-4][0-9]|" +
            "1[0-9]{2}|[0-9]{1,2})\\.){2}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[0-9]{1,2})\\]?\$)")
}

fun String.matchWithUsername(): Boolean {
    return this.matchWith("^(?=.{4,50}\$)(?=.*[a-z])[a-z0-9_]+\$")
}

fun String.matchWithPhone(): Boolean {
    return this.matchWith("^(?=.{8,30}\$)[0-9]+\$")
}

fun String.matchWithPassword(): Boolean {
    return this.matchWith("^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d])[A-Za-z\\d!@#\$%^&*()_+=-`~]{8,50}\$")
}
