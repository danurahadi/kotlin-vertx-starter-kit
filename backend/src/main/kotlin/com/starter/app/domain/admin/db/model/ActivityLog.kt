package com.starter.app.domain.admin.db.model

import com.starter.app.domain.admin.plain.ActivityLogList
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.Cache
import io.ebean.annotation.WhenCreated
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent activity_logs table in the DB
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "activity_logs")
@Cache(enableBeanCache = true, enableQueryCache = true)
class ActivityLog() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(15),
        description: String,
        actor: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.description = description
        this.actor = actor
    }

    @Id
    var id: Long? = 0

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 characters.")
    @Pattern(
        regexp = "^[a-z0-9]+$",
        message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var externalId: String

    @Lob
    @NotBlank(message = "Description could not be blank.")
    lateinit var description: String

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @ManyToOne(fetch = FetchType.EAGER)
    var actor: Admin? = null

    fun toActivityLogList(timezoneOffset: Long): ActivityLogList {
        return ActivityLogList(
            id = externalId,
            description = description,
            createdAt = createdAt.plusMinutes(timezoneOffset).toString(),
            actor = actor?.toLogActor()
        )
    }
}
