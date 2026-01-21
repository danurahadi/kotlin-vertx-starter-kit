package com.starter.app.domain.user.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.db.model.ApiKey
import com.starter.app.domain.auth.db.model.Role
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import com.starter.app.domain.user.db.model.value.OnlineStatus
import com.starter.app.domain.user.plain.LogActor
import com.starter.app.domain.user.plain.UserDetail
import com.starter.app.domain.user.plain.UserList
import com.starter.app.domain.user.plain.UserResponse
import id.yoframework.extra.snowflake.nextAlpha
import id.yoframework.web.security.SecurityModel
import io.ebean.annotation.Cache
import io.ebean.annotation.DbDefault
import io.ebean.annotation.DocCode
import io.ebean.annotation.DocStore
import io.ebean.annotation.DocStoreMode
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent users table in the DB.
 * This class is an implementation of [SecurityModel] because will be used in authentication process.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "users")
@DocStore(persist = DocStoreMode.QUEUE)
@Cache(enableBeanCache = true, enableQueryCache = true)
class CmsUser() : SecurityModel {
    constructor(
        id: Long? = null,
        role: Role,
        externalId: String = nextAlpha(17),
        username: String,
        password: String,
        email: String,
        emailVerified: Boolean = false,
        newEmail: String? = null,
        phone: String?,
        phoneVerified: Boolean = false,
        token: String? = null,
        tokenExpiredOn: LocalDateTime? = LocalDateTime.now().plusHours(24),
        tokenPhone: String? = null,
        tokenPhoneExpiredOn: LocalDateTime? = null,
        forgotCode: String? = null,
        forgotCodeExpiredOn: LocalDateTime? = null,
        login2FACode: String? = null,
        login2FACodeExpiredOn: LocalDateTime? = null,
        lastSendToken: LocalDateTime? = null,
        lastSendTokenPhone: LocalDateTime? = null,
        lastSend2FACode: LocalDateTime? = null,
        lastLogin: LocalDateTime? = null,
        lastSeen: LocalDateTime? = null,
        onlineStatus: OnlineStatus = OnlineStatus.AWAY,
        status: CmsUserStatus = CmsUserStatus.PENDING,
        locked: Boolean = false,
        autoUnlockedAt: LocalDateTime? = null,
        loginAttempt: Int = 0,
        createdBy: CmsUser? = null,
        lastUpdatedBy: CmsUser? = null
    ) : this() {
        this.id = id
        this.role = role
        this.externalId = externalId
        this.username = username
        this.password = password
        this.email = email
        this.emailVerified = emailVerified
        this.newEmail = newEmail
        this.phone = phone
        this.phoneVerified = phoneVerified
        this.token = token
        this.tokenExpiredOn = tokenExpiredOn
        this.tokenPhone = tokenPhone
        this.tokenPhoneExpiredOn = tokenPhoneExpiredOn
        this.forgotCode = forgotCode
        this.forgotCodeExpiredOn = forgotCodeExpiredOn
        this.login2FACode = login2FACode
        this.login2FACodeExpiredOn = login2FACodeExpiredOn
        this.lastSendToken = lastSendToken
        this.lastSendTokenPhone = lastSendTokenPhone
        this.lastSend2FACode = lastSend2FACode
        this.lastLogin = lastLogin
        this.lastSeen = lastSeen
        this.onlineStatus = onlineStatus
        this.status = status
        this.locked = locked
        this.autoUnlockedAt = autoUnlockedAt
        this.loginAttempt = loginAttempt
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var role: Role

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 chars.")
    @Pattern(
        regexp = "^[a-z0-9]+\$",
        message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var externalId: String

    @Column(unique = true)
    @NotBlank(message = "Email could not be blank.")
    @Email(message = "Invalid email format. Please supply valid email address.")
    @Size(max = 100, message = "Email could not be more than 100 chars.")
    lateinit var email: String

    @Column(unique = true)
    @NotBlank(message = "Username could not be blank.")
    @Size(min = 4, max = 50, message = "Username length must be between 4 - 50 chars.")
    @Pattern(
        regexp = "^[a-z0-9_]+\$",
        message = "Username can only contain lowercase alphanumeric characters " +
                "(letters A-Z, numbers 0-9) and underscores."
    )
    lateinit var username: String

    @DocCode
    @NotBlank(message = "Password could not be blank.")
    @Size(max = 500, message = "Password could not be more than 500 chars.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[\\d])[A-Za-z\\d\\W]{8,100}\$",
        message = "Password must contain at least one uppercase, one lowercase, one numeral, and eight characters."
    )
    lateinit var password: String

    @DocCode
    var emailVerified: Boolean = false

    @Column(unique = true)
    @Email(message = "Invalid email format. Please supply valid email address.")
    @Size(max = 100, message = "Email could not be more than 100 chars.")
    var newEmail: String? = null

    @DocCode
    @Column(unique = true)
    @Size(min = 8, max = 50, message = "Phone number length must be between 8 - 50 characters.")
    @Pattern(regexp = "^[0-9+]+\$", message = "Phone number can only contain numeric characters (0-9).")
    var phone: String? = null

    @DocCode
    var phoneVerified: Boolean = false

    @DocCode
    @Size(max = 20, message = "Token could not be more than 20 chars.")
    var token: String? = null

    @DocCode
    var tokenExpiredOn: LocalDateTime? = null

    @DocCode
    @Size(max = 10, message = "Token phone could not be more than 10 chars.")
    var tokenPhone: String? = null

    @DocCode
    var tokenPhoneExpiredOn: LocalDateTime? = null

    @DocCode
    @Size(max = 20, message = "Forgot code could not be more than 20 chars.")
    var forgotCode: String? = null

    @DocCode
    var forgotCodeExpiredOn: LocalDateTime? = null

    @DocCode
    @Size(max = 10, message = "Login 2FA code could not be more than 10 chars.")
    var login2FACode: String? = null

    @DocCode
    var login2FACodeExpiredOn: LocalDateTime? = null

    @DocCode
    var lastSendToken: LocalDateTime? = null

    @DocCode
    var lastSendTokenPhone: LocalDateTime? = null

    @DocCode
    var lastSend2FACode: LocalDateTime? = null

    @DocCode
    var lastLogin: LocalDateTime? = null

    @DocCode
    var lastSeen: LocalDateTime? = null

    @DocCode
    @DbDefault(value = "AWAY")
    var onlineStatus: OnlineStatus = OnlineStatus.AWAY

    @DocCode
    var status: CmsUserStatus = CmsUserStatus.ACTIVE

    @DocCode
    var locked: Boolean = false

    @DocCode
    var autoUnlockedAt: LocalDateTime? = null

    @DbDefault(value = "0")
    @Min(value = 0, message = "Login attempt could not be less than 0.")
    @Max(value = 50, message = "Login attempt could not be more than 50.")
    var loginAttempt: Int = 0

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: CmsUser? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: CmsUser? = null

    @DocCode
    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @DocCode
    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    @OneToOne(mappedBy = "cmsUser", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    var apiKey: ApiKey? = null

    @OneToOne(mappedBy = "cmsUser", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    lateinit var admin: Admin

    @OneToOne(mappedBy = "cmsUser", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    lateinit var setting: CmsUserSetting

    override fun permission(): Set<String> {
        return setOf("SUPERADMIN", "ADMIN")
    }

    override fun toJsonObject(): JsonObject {
        return json {
            obj(
                "id" to id,
                "email" to email,
                "username" to username,
                "phone" to phone,
                "status" to status
            )
        }
    }

    fun toLogActor(): LogActor {
        return LogActor(
            id = externalId,
            username = username,
            fullName = admin.fullName,
            thumbnailProfileImage = admin.thumbnailProfileImage
        )
    }

    fun toUserList(): UserList {
        return UserList(
            id = externalId,
            email = email,
            username = username,
            phone = phone,
            emailVerified = emailVerified,
            roleName = role.name,
            locked = locked,
            status = status,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }

    fun toUserDetail(): UserDetail {
        return UserDetail(
            id = externalId,
            email = email,
            username = username,
            phone = phone,
            newEmail = newEmail,
            emailVerified = emailVerified,
            phoneVerified = phoneVerified,
            lastLogin = lastLogin?.toString(),
            roleName = role.name,
            locked = locked,
            status = status,
            lastSendToken = lastSendToken?.toString(),
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }

    fun toResponseCompact(): UserResponse {
        return UserResponse(
            user = this.toUserList(),
            admin = admin.toAdminList()
        )
    }

    fun toResponseDetail(): UserResponse {
        return UserResponse(
            user = this.toUserDetail(),
            admin = admin.toAdminDetail(),
            settings = setting.toUserSettings()
        )
    }
}
