package com.starter.app.domain.auth.db.repository

import com.starter.app.domain.auth.db.model.AccessRole
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for AccessRole bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AccessRoleRepository @Inject constructor(ebeanServer: Database):
    Repository<AccessRole, Long>(ebeanServer, AccessRole::class) {

    suspend fun findByAccessId(accessId: Long?): Query<AccessRole> {
        val accessID = accessId orDataError "Invalid access ID."
        return query
            .select("*")
            .where()
            .eq("access.id", accessID)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByRoleId(roleId: Long?): Query<AccessRole> {
        val roleID = roleId orDataError "Invalid role ID."
        return query
            .select("*")
            .where()
            .eq("role.id", roleID)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByAccessAndRole(accessId: Long?, roleId: Long?): Query<AccessRole> {
        val accessID = accessId orDataError "Invalid access ID."
        val roleID = roleId orDataError "Invalid role ID."
        return query
            .select("*")
            .where()
            .and()
                .eq("access.id", accessID)
                .eq("role.id", roleID)
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByModuleIdAndRoleId(moduleId: Long?, roleId: Long?): Query<AccessRole> {
        val roleID = roleId orDataError "Invalid role ID."
        val moduleID = moduleId orDataError "Invalid module ID."
        return query
            .select("*")
            .where()
            .and()
                .eq("access.module.id", moduleID)
                .eq("role.id", roleID)
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByAccessNameAndRoleNameAndPermission(
        accessName: String,
        roleName: String,
        permission: AccessRolePermission
    ): Query<AccessRole> {
        return query
            .select("id")
            .fetchLazy("access", "id")
            .fetchLazy("role", "id")
            .fetchLazy("createdBy", "id")
            .fetchLazy("lastUpdatedBy", "id")
            .where()
            .and()
                .eq("access.name", accessName)
                .eq("role.name", roleName)
                .eq("permission", permission)
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findAllowedAccessByRole(roleId: Long?): Query<AccessRole> {
        val roleID = roleId orDataError "Invalid role ID."
        return query
            .select("*")
            .where()
            .and()
                .eq("role.id", roleID)
                .eq("permission", AccessRolePermission.ALLOWED)
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByRoleIdAndPermission(
        roleId: Long?,
        moduleId: Long? = null,
        permission: AccessRolePermission? = null,
        keyword: String
    ): Query<AccessRole> {

        val roleID = roleId orDataError "Invalid role ID."
        val q = query
            .select("*")
            .where()
            .and()
                .eq("role.id", roleID)

        return q.run {
            if (permission == null) {
                this
            } else {
                this
                    .eq("permission", permission)
            }
        }
            .run {
                if (moduleId == null) {
                    this
                } else {
                    this
                        .eq("access.module.id", moduleId)
                }
            }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .or()
                            .icontains("access.name", keyword)
                            .icontains("access.alias", keyword)
                        .endOr()
                }
            }
            .run {
                this
                    .query()
                    .setAutoTune(false)
            }
    }

    suspend fun findByAccessIdAndPermission(
        accessId: Long?,
        permission: AccessRolePermission? = null,
        keyword: String
    ): Query<AccessRole> {

        val accessID = accessId orDataError "Invalid access ID."
        val q = query
            .select("*")
            .where()
            .and()
                .eq("access.id", accessID)
                .ne("role.name", "SUPERADMIN")

        return q.run {
            if (permission == null) {
                this
            } else {
                this
                    .eq("permission", permission)
            }
        }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .or()
                            .icontains("role.name", keyword)
                            .icontains("role.alias", keyword)
                        .endOr()
                }
            }
            .run {
                this
                    .query()
                    .setAutoTune(false)
            }
    }
}
