package com.starter.app.domain.auth.db.model

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import com.starter.app.domain.auth.plain.AccessRoleDetail
import com.starter.app.domain.auth.plain.AccessRoleList
import com.starter.app.domain.auth.plain.RoleAccessList
import id.yoframework.core.model.Model
import io.ebean.annotation.WhenCreated
import io.ebean.annotation.WhenModified
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Model that represent access_roles intermediate table in the DB
 * as the result of Many-to-many relationship between access & roles table.
 * But, in the api we don't use ManyToMany annotation between that entities
 * and use the hasMany through relation instead.
 *
 * Access OneToMany AccessRole and Role OneToMany AccessRole.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "access_roles")
class AccessRole() : Model {
    constructor(
        id: Long? = null,
        access: Access,
        role: Role,
        permission: AccessRolePermission = AccessRolePermission.DENIED,
        createdBy: Admin? = null,
        lastUpdatedBy: Admin? = null
    ) : this() {
        this.id = id
        this.access = access
        this.role = role
        this.permission = permission
        this.createdBy = createdBy
        this.lastUpdatedBy = lastUpdatedBy
    }

    @Id
    var id: Long? = null

    @ManyToOne(fetch = FetchType.EAGER)
    lateinit var access: Access

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

    fun toAccessList(): RoleAccessList {
        return RoleAccessList(
            id = access.externalId,
            name = access.name,
            alias = access.alias,
            permission = permission,
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toRoleList(): AccessRoleList {
        return AccessRoleList(
            id = role.externalId,
            name = role.name,
            alias = role.alias,
            permission = permission,
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }

    fun toAccessRoleDetail(): AccessRoleDetail {
        return AccessRoleDetail(
            access = access.toAccessDetail(),
            role = role.toRoleDetail(),
            permission = permission,
            lastUpdatedAt = lastUpdatedAt.toString()
        )
    }
}
