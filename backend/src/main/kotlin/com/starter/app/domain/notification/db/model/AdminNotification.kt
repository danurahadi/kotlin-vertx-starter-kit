package com.starter.app.domain.notification.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.notification.db.model.value.NotificationStatus
import com.starter.app.domain.notification.plain.AdminNotificationList
import com.starter.app.domain.user.db.model.CmsUser
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.WhenCreated
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent admin_notifications table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "admin_notifications")
class AdminNotification() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(33),
        admin: Admin,
        message: String,
        status: NotificationStatus = NotificationStatus.NEW,
        actor: CmsUser? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.admin = admin
        this.message = message
        this.status = status
        this.actor = actor
    }

    @Id
    var id: Long? = 0

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 characters.")
    @Pattern(
        regexp = "^[a-z0-9]+\$",
        message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var externalId: String

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var admin: Admin

    @Lob
    @NotBlank(message = "Message could not be blank.")
    lateinit var message: String

    lateinit var status: NotificationStatus

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @ManyToOne(fetch = FetchType.EAGER)
    var actor: CmsUser? = null

    fun toAdminNotificationList(): AdminNotificationList {
        return AdminNotificationList(
            id = externalId,
            message = message,
            status = status,
            createdAt = createdAt.toString(),
            actor = actor?.toLogActor()
        )
    }
}
