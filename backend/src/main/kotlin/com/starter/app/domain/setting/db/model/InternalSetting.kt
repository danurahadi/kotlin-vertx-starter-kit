package com.starter.app.domain.setting.db.model

import id.yoframework.core.model.Model
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent internal_settings table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "internal_settings")
class InternalSetting() : Model {
    constructor(
        id: Long? = null,
        settingKey: String,
        settingValue: String
    ) : this() {
        this.id = id
        this.settingKey = settingKey
        this.settingValue = settingValue
    }

    @Id
    var id: Long? = 0

    @Column(unique = true)
    @NotBlank(message = "Setting key could not be blank.")
    @Size(max = 150, message = "Setting key could not be more than 150 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9-_& ]+\$",
        message = "Setting key can only contain alphanumeric characters " +
                "(letters A-Z, numbers 0-9), underscore, and space."
    )
    lateinit var settingKey: String

    @NotBlank(message = "Setting value could not be blank.")
    @Size(max = 50, message = "Setting value could not be more than 50 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9]+\$",
        message = "Setting value can only contain alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var settingValue: String

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime
}
