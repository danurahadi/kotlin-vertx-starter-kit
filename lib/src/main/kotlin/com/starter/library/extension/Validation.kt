package com.starter.library.extension

import id.yoframework.core.model.Model
import id.yoframework.web.exception.ValidationException
import io.ebean.Database
import jakarta.validation.Validation
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator
import java.util.*

/**
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

/**
 * Validate a bean entity that implement @Model interface using Bean Validation (Hibernate)
 *
 * @return Boolean true if no constraint violations returned from the validator
 * @throws [ValidationException] with all violation messages as List<String>
 *
 */
fun Model.validate(): Boolean {
//    val factory = Validation.buildDefaultValidatorFactory()
    val factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()

    val validator = factory.validator
    val violations = validator.validate(this)

    if (violations.isNotEmpty()) {
        val validationMessages = violations.toList()
            .map {
                it.message
            }
        throw ValidationException(validationMessages)
    }

    return true
}

/**
 * Validate bean entity list that implement @Model interface using Bean Validation (Hibernate)
 *
 * @return Boolean true if no constraint violations returned from the validator
 * @throws [ValidationException] with all violation messages as List<String>
 *
 */
fun List<Model>.validate(): Boolean {
    val factory = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(ParameterMessageInterpolator())
        .buildValidatorFactory()

    val validator = factory.validator
    val objectList = this

    objectList.forEach { obj ->
        val violations = validator.validate(obj)
        if (violations.isNotEmpty()) {
            val validationMessages = violations
                .map {
                    it.message
                }
            throw ValidationException(validationMessages)
        }
    }

    return true
}

/**
 * Validate uniqueness of a bean entity that implement [Model] interface using Ebean method
 *
 * @return Boolean true if no constraint violations returned from the checkUniqueness method
 * @throws [ValidationException] with all violation messages as List<String>
 *
 */
fun Model.validateUnique(ebeanDatabase: Database): Boolean {
    val properties = ebeanDatabase.checkUniqueness(this)

    if (properties.isNotEmpty()) {
        val validationMessages = properties.toList()
            .map { p ->
                val propName = p.name().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
                }
                "$propName '${p.value(this)}' already exist. Try another."
            }
        throw ValidationException(validationMessages)
    }

    return true
}

/**
 * Validate uniqueness of a List bean entity that implement [Model] interface using Ebean method
 *
 * @return Boolean true if no constraint violations returned from the checkUniqueness method
 * @throws [ValidationException] with all violation messages as List<String>
 *
 */
fun List<Model>.validateUnique(ebeanDatabase: Database): Boolean {
    this.forEach { m ->
        val properties = ebeanDatabase.checkUniqueness(m)

        if (properties.isNotEmpty()) {
            val validationMessages = properties.toList()
                .map { p ->
                    val propName = p.name().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
                    }
                    "$propName '${p.value(m)}' already exist. Try another."
                }
            throw ValidationException(validationMessages)
        }
    }

    return true
}

