package com.starter.app.domain.auth.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.plain.RoleDetail
import com.starter.app.domain.auth.plain.RoleList
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
 * Model (bean entity) that represent roles table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "roles")
class Role() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(3),
        name: String,
        alias: String,
        description: String? = null,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.name = name
        this.alias = alias
        this.description = description
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = null

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 chars.")
    @Pattern(regexp = "^[a-z0-9]+\$", message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9).")
    lateinit var externalId: String

    @Column(unique = true)
    @NotBlank(message = "Name could not be blank.")
    @Size(max = 50, message = "Name could not be more than 50 characters.")
    @Pattern(regexp = "^[A-Z-]+\$", message = "Name can only contain uppercase letter characters (letters A-Z) and dash.")
    lateinit var name: String

    @NotBlank(message = "Alias could not be blank.")
    @Size(max = 50, message = "Alias could not be more than 50 characters.")
    @Pattern(regexp = "^[A-Za-z0-9-& ]+\$", message = "Alias can only contain alphanumeric characters (letters A-Z, numbers 0-9), dash, and space.")
    lateinit var alias: String

    @Size(max = 255, message = "Description could not be more than 255 characters.")
    var description: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toRoleList(): RoleList {
        return RoleList(
            id = externalId,
            name = name,
            alias = alias,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toRoleDetail(): RoleDetail {
        return RoleDetail(
            id = externalId,
            name = name,
            alias = alias,
            description = description,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }
}
