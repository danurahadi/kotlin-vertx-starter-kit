package com.starter.library.error

import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.exception.NullObjectException
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.InvalidCredentials
import id.yoframework.web.exception.NotAllowedException
import id.yoframework.web.exception.NotFoundException
import id.yoframework.web.exception.SecurityException
import id.yoframework.web.exception.UnauthorizedException
import id.yoframework.web.exception.ValidationException
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.FileNotFoundException

fun Throwable.toErrorsCode(): Int {
    return when (this) {
        is FileNotFoundException -> HttpResponseStatus.NOT_FOUND.code()
        is NullObjectException -> HttpResponseStatus.NOT_FOUND.code()
        is DataInconsistentException -> HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
        is NotAllowedException -> HttpResponseStatus.METHOD_NOT_ALLOWED.code()
        is SecurityException -> HttpResponseStatus.UNAUTHORIZED.code()
        is ValidationException -> HttpResponseStatus.UNPROCESSABLE_ENTITY.code()
        is BadRequestException -> HttpResponseStatus.BAD_REQUEST.code()
        is UnauthorizedException -> HttpResponseStatus.UNAUTHORIZED.code()
        is NotFoundException -> HttpResponseStatus.NOT_FOUND.code()
        is InvalidCredentials -> HttpResponseStatus.FORBIDDEN.code()
        else -> 500
    }
}

fun Int.toException(message: String): Exception {
    return when (this) {
        400 -> BadRequestException(message)
        401 -> UnauthorizedException(message)
        403 -> InvalidCredentials(message)
        404 -> NotFoundException(message)
        405 -> NotAllowedException(message)
        422 -> ValidationException(listOf(message))
        500 -> DataInconsistentException(message)
        else -> DataInconsistentException(message)
    }
}
