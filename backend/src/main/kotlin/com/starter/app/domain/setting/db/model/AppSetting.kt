package com.starter.app.domain.setting.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.setting.db.model.value.SettingType
import com.starter.app.domain.setting.plain.AppSettingDetail
import com.starter.app.domain.setting.plain.AppSettingList
import com.starter.app.domain.setting.plain.SettingGroupDetail
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.DbArray
import io.ebean.annotation.DbDefault
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent app_settings table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "app_settings")
class AppSetting() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(11),
        settingGroup: SettingGroup,
        settingKey: String,
        settingValue: String,
        settingType: SettingType,
        settingOptions: List<String>?,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.settingGroup = settingGroup
        this.settingKey = settingKey
        this.settingValue = settingValue
        this.settingType = settingType
        this.settingOptions = settingOptions
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
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

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var settingGroup: SettingGroup

    @Column(unique = true)
    @NotBlank(message = "Setting key could not be blank.")
    @Size(max = 150, message = "Setting key could not be more than 150 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9-_& ]+$",
        message = "Setting key can only contain alphanumeric characters " +
                "(letters A-Z, numbers 0-9), underscore, and space.")
    lateinit var settingKey: String

    @NotBlank(message = "Setting value could not be blank.")
    @Size(max = 50, message = "Setting value could not be more than 50 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9]+$",
        message = "Setting value can only contain alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var settingValue: String

    @DbDefault(value = "BOOLEAN")
    lateinit var settingType: SettingType

    @DbArray
    var settingOptions: List<String>? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toAppSettingList(): AppSettingList {
        return AppSettingList(
            id = externalId,
            settingGroupName = settingGroup.name,
            settingKey = settingKey,
            settingValue = settingValue,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toAppSettingDetail(): AppSettingDetail {
        return AppSettingDetail(
            id = externalId,
            settingGroup = SettingGroupDetail(
                id = settingGroup.externalId,
                name = settingGroup.name,
                createdAt = settingGroup.createdAt.toString(),
                lastUpdatedAt = settingGroup.lastUpdatedAt.toString(),
                createdBy = settingGroup.createdBy?.toLogActor(),
                lastUpdatedBy = settingGroup.lastUpdatedBy?.toLogActor()
            ),
            settingKey = settingKey,
            settingValue = settingValue,
            settingType = settingType,
            settingOptions = settingOptions,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }
}
