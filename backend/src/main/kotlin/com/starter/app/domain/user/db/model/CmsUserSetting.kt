package com.starter.app.domain.user.db.model

import com.starter.app.domain.user.plain.UserSettings
import id.yoframework.core.model.Model
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent user_settings table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "user_settings")
class CmsUserSetting() : Model {
    constructor(
        id: Long? = null,
        cmsUser: CmsUser,
        timezoneOffset: Long,
    ) : this() {
        this.id = id
        this.cmsUser = cmsUser
        this.timezoneOffset = timezoneOffset
    }

    @Id
    var id: Long? = 0

    @OneToOne(fetch = FetchType.EAGER)
    lateinit var cmsUser: CmsUser

    @Min(value = -780, message = "Timezone offset could not be less than -780.")
    @Max(value = 780, message = "Timezone offset could not be more than 780.")
    var timezoneOffset: Long = 420

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toUserSettings(): UserSettings {
        return UserSettings(
            timezoneOffset = timezoneOffset
        )
    }
}
