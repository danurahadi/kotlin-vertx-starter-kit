package com.starter.app.domain.auth.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.plain.ApiKeyList
import com.starter.app.domain.user.db.model.CmsUser
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.DbDefault
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent api_keys table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "api_keys")
class ApiKey() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(2),
        cmsUser: CmsUser?,
        title: String,
        ipAddress: String,
        location: String,
        browser: String? = null,
        operatingSystem: String? = null,
        expiredTime: LocalDateTime? = null,
        accessToken: String,
        regToken: String? = null,
        imei: String? = null,
        createdBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.cmsUser = cmsUser
        this.title = title
        this.accessToken = accessToken
        this.expiredTime = expiredTime
        this.ipAddress = ipAddress
        this.location = location
        this.browser = browser
        this.operatingSystem = operatingSystem
        this.regToken = regToken
        this.imei = imei
        this.createdBy = createdBy
    }

    @Id
    var id: Long? = null

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 chars.")
    @Pattern(regexp = "^[a-z0-9]+\$", message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9).")
    lateinit var externalId: String

    @OneToOne(fetch = FetchType.EAGER)
    var cmsUser: CmsUser? = null

    @DbDefault(value = "Internal Client App")
    @NotBlank(message = "Title could not be blank.")
    @Size(max = 150, message = "Title could not be more than 150 characters.")
    lateinit var title: String

    @NotBlank(message = "Access token could not be blank.")
    @Size(max = 510, message = "Access token could not be more than 510 characters.")
    lateinit var accessToken: String

    var expiredTime: LocalDateTime? = null

    @Size(max = 50, message = "IP Address could not be more than 50 characters.")
    @Pattern(regexp = "^[a-z0-9:.]+\$", message = "IP Address can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9), dot, and colon.")
    lateinit var ipAddress: String

    @Size(max = 50, message = "Location could not be more than 50 characters.")
    lateinit var location: String

    @Size(max = 255, message = "Browser could not be more than 255 characters.")
    var browser: String? = null

    @Size(max = 255, message = "Operating system could not be more than 255 characters.")
    var operatingSystem: String? = null

    @Size(max = 510, message = "Reg token could not be more than 510 characters.")
    var regToken: String? = null

    @Size(max = 16, message = "IMEI could not be more than 16 characters.")
    @Pattern(regexp = "^[0-9]+\$", message = "IMEI can only contain numeric characters (numbers 0-9).")
    var imei: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toApiKeyList(): ApiKeyList {
        return ApiKeyList(
            id = externalId,
            title = title,
            accessToken = accessToken,
            createdAt = createdAt.toString(),
            expiredAt = expiredTime?.toString()
        )
    }
}
