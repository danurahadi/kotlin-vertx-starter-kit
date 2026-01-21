package com.starter.app.domain.setting.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.setting.plain.CountryCompact
import com.starter.app.domain.setting.plain.CountryList
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.*
import io.ebean.annotation.Cache
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent countries table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "countries")
@Cache(enableBeanCache = false, enableQueryCache = false)
class Country() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(5),
        alpha2Code: String? = null,
        alpha3Code: String? = null,
        name: String,
        callingCode: String? = null,
        geonameId: String? = null,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.alpha2Code = alpha2Code
        this.alpha3Code = alpha3Code
        this.name = name
        this.callingCode = callingCode
        this.geonameId = geonameId
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = 0

    @DocCode
    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 characters.")
    @Pattern(
        regexp = "^[a-z0-9]+$",
        message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var externalId: String

    @DocCode
    @Size(max = 2, message = "Alpha 2 code could not be more than 2 characters.")
    @Pattern(
        regexp = "^[A-Z]+$",
        message = "Alpha 2 code can only contain uppercase letters."
    )
    var alpha2Code: String? = null

    @DocCode
    @Size(max = 3, message = "Alpha 3 code could not be more than 3 characters.")
    @Pattern(
        regexp = "^[A-Z]+$",
        message = "Alpha 3 code can only contain uppercase letters."
    )
    var alpha3Code: String? = null

    @DocSortable
    @Column(unique = true)
    @NotBlank(message = "Name could not be blank.")
    @Size(max = 100, message = "Name could not be more than 100 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9 ]+$",
        message = "Name can only contain alphanumeric characters (letters A-Z, numbers 0-9) and space."
    )
    lateinit var name: String

    @DocCode
    @NotBlank(message = "Calling code could not be blank.")
    @Size(max = 10, message = "Calling code could not be more than 10 characters.")
    @Pattern(
        regexp = "^[0-9+ ]+$",
        message = "Calling code can only contain numeric characters (numbers 0-9), space, and + sign."
    )
    var callingCode: String? = null

    var currency: String? = null
    var currencySymbol: String? = null
    var geonameId: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @DocCode
    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @DocCode
    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toCountryCompact(): CountryCompact {
        return CountryCompact(
            id = externalId,
            name = name,
            alpha2Code = alpha2Code,
            callingCode = callingCode
        )
    }

    fun toCountryList(): CountryList {
        return CountryList(
            id = externalId,
            alpha2Code = alpha2Code,
            name = name,
            callingCode = callingCode,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }
}
