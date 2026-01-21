package com.starter.app.domain.setting.db.model

import com.starter.app.domain.setting.db.model.value.BannerType
import com.starter.app.domain.setting.plain.BannerResponseDetail
import com.starter.app.domain.setting.plain.BannerResponseList
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.Cache
import io.ebean.annotation.DbDefault
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
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "banners")
@Cache(enableBeanCache = true, enableQueryCache = true)
class Banner() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(7),
        title: String,
        description: String? = null,
        image: String? = null,
        linkUrl: String? = null,
        type: BannerType,
        sequence: Int,
        status: Boolean = false
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.title = title
        this.description = description
        this.image = image
        this.linkUrl = linkUrl
        this.type = type
        this.sequence = sequence
        this.status = status
    }

    @Id
    var id: Long? = 0

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 chars.")
    @Pattern(
        regexp = "^[a-z0-9]+\$",
        message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9)."
    )
    lateinit var externalId: String

    lateinit var title: String
    var description: String? = null
    var image: String? = null

    var linkUrl: String? = null
    lateinit var type: BannerType

    @DbDefault(value = "0")
    var sequence: Int = 0

    var status: Boolean = false

    @WhenCreated
    var createdAt: LocalDateTime? = null

    @WhenModified
    var updatedAt: LocalDateTime? = null

    fun toJsonObject(): BannerResponseList {
        return BannerResponseList(
            id = id?.toString(),
            title = title,
            image = image,
            linkUrl = linkUrl,
            type = type.toString().replace("_BANNER", ""),
            sequence = sequence,
            status = status,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString()
        )
    }

    fun toJsonObjectDetail(): BannerResponseDetail {
        return BannerResponseDetail(
            id = id?.toString(),
            title = title,
            description = description,
            image = image,
            linkUrl = linkUrl,
            type = type.toString().replace("_BANNER", ""),
            sequence = sequence,
            status = status,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString()
        )
    }
}
