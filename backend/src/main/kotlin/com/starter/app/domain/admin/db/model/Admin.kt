package com.starter.app.domain.admin.db.model

import com.starter.app.domain.admin.plain.AdminCompact
import com.starter.app.domain.admin.plain.AdminDetail
import com.starter.app.domain.admin.plain.AdminList
import com.starter.app.domain.user.db.model.CmsUser
import com.starter.app.domain.user.db.model.value.Gender
import com.starter.app.domain.user.plain.LogActor
import com.starter.app.domain.user.plain.UserResponse
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.Cache
import io.ebean.annotation.DbDefault
import io.ebean.annotation.DocCode
import io.ebean.annotation.DocStore
import io.ebean.annotation.DocStoreMode
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent admins table in the DB
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "admins")
@DocStore(persist = DocStoreMode.QUEUE)
@Cache(enableBeanCache = true, enableQueryCache = true)
class Admin(): Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(18),
        cmsUser: CmsUser,
        fullName: String,
        thumbnailProfileImage: String? = null,
        originalProfileImage: String? = null,
        gender: Gender? = null,
        birthday: LocalDate? = null,
        complete: Boolean = false,
        superadmin: Boolean = false
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.cmsUser = cmsUser
        this.fullName = fullName
        this.thumbnailProfileImage = thumbnailProfileImage
        this.originalProfileImage = originalProfileImage
        this.gender = gender
        this.birthday = birthday
        this.complete = complete
        this.superadmin = superadmin
    }

    @Id
    var id: Long? = 0

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    lateinit var cmsUser: CmsUser

    @DocCode
    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 characters.")
    @Pattern(
        regexp = "^[a-z0-9]+\$",
        message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var externalId: String

    @DbDefault(value = "")
    @NotBlank(message = "Full name could not be blank.")
    @Size(max = 200, message = "Full name could not be more than 100 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9-& ]+\$",
        message = "Full name can only contain alphanumeric characters (letters A-Z, numbers 0-9) and space."
    )
    lateinit var fullName: String

    @DocCode
    @URL
    @Size(max = 255, message = "Thumbnail profile image could not be more than 255 characters.")
    var thumbnailProfileImage: String? = null

    @DocCode
    @URL
    @Size(max = 255, message = "Original profile image could not be more than 255 characters.")
    var originalProfileImage: String? = null

    @DocCode
    var gender: Gender? = null

    @DocCode
    var birthday: LocalDate? = null

    @DocCode
    var complete: Boolean = false

    @DocCode
    var superadmin: Boolean = false

    @DocCode
    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @DocCode
    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toLogActor(): LogActor {
        return LogActor(
            id = externalId,
            username = cmsUser.username,
            fullName = fullName,
            thumbnailProfileImage = thumbnailProfileImage
        )
    }

    fun toAdminCompact(): AdminCompact {
        return AdminCompact(
            id = externalId,
            username = cmsUser.username,
            fullName = fullName,
            thumbnailProfileImage = thumbnailProfileImage
        )
    }

    fun toAdminList(): AdminList {
        return AdminList(
            id = externalId,
            fullName = fullName,
            thumbnailProfileImage = thumbnailProfileImage,
            complete = complete,
            superadmin = superadmin,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toAdminDetail(unreadNotifsCount: Int = 0): AdminDetail {
        return AdminDetail(
            id = externalId,
            fullName = fullName,
            thumbnailProfileImage = thumbnailProfileImage,
            originalProfileImage = originalProfileImage,
            gender = gender,
            birthday = birthday,
            complete = complete,
            superadmin = superadmin,
            unreadNotifsCount = unreadNotifsCount,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toResponseCompact(): UserResponse {
        return UserResponse(
            user = cmsUser.toUserList(),
            admin = this.toAdminList()
        )
    }

    fun toResponseDetail(unreadNotifsCount: Int = 0): UserResponse {
        return UserResponse(
            user = cmsUser.toUserDetail(),
            admin = this.toAdminDetail(
                unreadNotifsCount = unreadNotifsCount
            ),
            settings = cmsUser.setting.toUserSettings()
        )
    }
}
