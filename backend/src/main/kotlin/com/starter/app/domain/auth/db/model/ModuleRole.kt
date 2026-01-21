package com.starter.app.domain.auth.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import com.starter.app.domain.auth.plain.ModuleRoleDetail
import com.starter.app.domain.auth.plain.ModuleRoleList
import com.starter.app.domain.auth.plain.RoleModuleList
import id.yoframework.core.model.Model
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Model that represent module_roles intermediate table in the DB
 * as the result of Many-to-many relationship between modules & roles table.
 * But, in the api we don't use ManyToMany annotation between that entities
 * and use the hasMany through relation instead.
 *
 * Module OneToMany ModuleRole and Role OneToMany ModuleRole.
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "module_roles")
class ModuleRole() : Model {
    constructor(
        id: Long? = null,
        module: Module,
        role: Role,
        permission: AccessRolePermission = AccessRolePermission.DENIED,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.module = module
        this.role = role
        this.permission = permission
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var module: Module

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var role: Role

    lateinit var permission: AccessRolePermission

    @ManyToOne(fetch = FetchType.EAGER)
    var createdBy: Admin? = null

    @ManyToOne(fetch = FetchType.EAGER)
    var lastUpdatedBy: Admin? = null

    @WhenCreated
    lateinit var createdAt: LocalDateTime

    @WhenModified
    lateinit var lastUpdatedAt: LocalDateTime

    fun toModuleList(): RoleModuleList {
        return RoleModuleList(
            id = module.externalId,
            code = module.code,
            name = module.name,
            permission = permission,
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toRoleList(): ModuleRoleList {
        return ModuleRoleList(
            id = role.externalId,
            name = role.name,
            alias = role.alias,
            permission = permission,
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toModuleRoleDetail(): ModuleRoleDetail {
        return ModuleRoleDetail(
            module = module.toModuleDetail(),
            role = role.toRoleDetail(),
            permission = permission,
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }
}
