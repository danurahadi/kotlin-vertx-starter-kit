package com.starter.app.domain.setting.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.setting.plain.AppSettingCompact
import com.starter.app.domain.setting.plain.SettingGroupCompact
import com.starter.app.domain.setting.plain.SettingGroupDetail
import com.starter.app.domain.setting.plain.SettingGroupList
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent setting_groups table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "setting_groups")
class SettingGroup() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(12),
        name: String,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.name = name
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

    @Column(unique = true)
    @NotBlank(message = "Name could not be blank.")
    @Size(max = 150, message = "Name could not be more than 150 characters.")
    @Pattern(
        regexp = "^[A-Za-z0-9-_& ]+$",
        message = "Name can only contain alphanumeric characters (letters A-Z, numbers 0-9) and space."
    )
    lateinit var name: String

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    @OneToMany(mappedBy = "settingGroup", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy(value = "settingKey ASC")
    lateinit var appSettings: List<AppSetting>

    fun toSettingGroupCompact(): SettingGroupCompact {
        return SettingGroupCompact(
            id = externalId,
            name = name,
            appSettings = appSettings.map { s ->
                AppSettingCompact(
                    id = s.externalId,
                    settingKey = s.settingKey,
                    settingValue = s.settingValue,
                    settingType = s.settingType,
                    settingOptions = s.settingOptions,
                    lastUpdatedAt = s.lastUpdatedAt.toString(),
                    lastUpdatedBy = lastUpdatedBy?.toLogActor()
                )
            }
        )
    }

    fun toSettingGroupList(): SettingGroupList {
        return SettingGroupList(
            id = externalId,
            name = name,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toSettingGroupDetail(): SettingGroupDetail {
        return SettingGroupDetail(
            id = externalId,
            name = name,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }
}
