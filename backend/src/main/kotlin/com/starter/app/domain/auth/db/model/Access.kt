package com.starter.app.domain.auth.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.plain.AccessDetail
import com.starter.app.domain.auth.plain.AccessList
import id.yoframework.core.model.Model
import id.yoframework.extra.snowflake.nextAlpha
import io.ebean.annotation.DbDefault
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * Model (bean entity) that represent access table in the DB
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "access")
class Access() : Model {
    constructor(
        id: Long? = null,
        externalId: String = nextAlpha(1),
        module: Module?,
        name: String,
        alias: String,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.externalId = externalId
        this.module = module
        this.name = name
        this.alias = alias
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = null

    @Column(unique = true)
    @NotBlank(message = "External ID could not be blank.")
    @Size(max = 16, message = "External ID could not be more than 16 chars.")
    @Pattern(regexp = "^[a-z0-9]+$", message = "External ID can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9).")
    lateinit var externalId: String

    @ManyToOne(fetch = FetchType.EAGER)
    var module: Module? = null

    @DbDefault(value = "")
    @Column(unique = true)
    @NotBlank(message = "Name could not be blank.")
    @Size(max = 150, message = "Name could not be more than 150 characters.")
    @Pattern(regexp = "^[a-z0-9-.]+$", message = "Name can only contain lowercase alphanumeric characters (letters A-Z, numbers 0-9), dot, and dash.")
    lateinit var name: String

    @DbDefault(value = "")
    @NotBlank(message = "Alias could not be blank.")
    @Size(max = 150, message = "Alias could not be more than 150 characters.")
    @Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "Alias can only contain alphanumeric characters (letters A-Z, numbers 0-9), and space.")
    lateinit var alias: String

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toAccessList(): AccessList {
        return AccessList(
            id = externalId,
            moduleName = module?.name,
            name = name,
            alias = alias,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toAccessDetail(): AccessDetail {
        return AccessDetail(
            id = externalId,
            module = module?.toModuleDetail(),
            name = name,
            alias = alias,
            createdAt = createdAt.toString(),
            lastUpdatedAt = lastUpdatedAt.toString(),
            createdBy = createdBy?.toLogActor(),
            lastUpdatedBy = lastUpdatedBy?.toLogActor()
        )
    }
}
