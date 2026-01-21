package com.starter.app.domain.auth.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.plain.ModuleCompact
import com.starter.app.domain.auth.plain.ModuleDetail
import com.starter.app.domain.auth.plain.ModuleList
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent modules table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "modules")
class Module() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(21),
        code: String,
        name: String,
        summary: String,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.code = code
        this.name = name
        this.summary = summary
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = 0

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 characters.")
    @Pattern(regexp = "^[a-z0-9]+\$", message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9).")
    lateinit var externalId: String

    @Column(unique = true)
    @NotBlank(message = "Code could not be blank.")
    @Size(max = 5, message = "Code could not be more than 3 characters.")
    @Pattern(regexp = "^[A-Za-z0-9]+\$", message = "Code can only contain alphanumeric characters (letters A-Z, numbers 0-9).")
    lateinit var code: String

    @Column(unique = true)
    @NotBlank(message = "Name could not be blank.")
    @Size(max = 100, message = "Name could not be more than 100 characters.")
    @Pattern(regexp = "^[A-Za-z0-9 ]+\$", message = "Name can only contain alphanumeric characters (letters A-Z, numbers 0-9) and space.")
    lateinit var name: String

    @NotBlank(message = "Summary could not be blank.")
    @Size(max = 255, message = "Summary could not be more than 255 characters.")
    @Pattern(regexp = "^[A-Za-z0-9.,-_& ]+\$", message = "Summary can only contain alphanumeric characters (letters A-Z, numbers 0-9) and space.")
    lateinit var summary: String

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toModuleCompact(): ModuleCompact {
        return ModuleCompact(
            id = externalId,
            code = code,
            name = name
        )
    }

    fun toModuleList(): ModuleList {
        return ModuleList(
            id = externalId,
            code = code,
            name = name,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toModuleDetail(): ModuleDetail {
        return ModuleDetail(
            id = externalId,
            code = code,
            name = name,
            summary = summary,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }
}
