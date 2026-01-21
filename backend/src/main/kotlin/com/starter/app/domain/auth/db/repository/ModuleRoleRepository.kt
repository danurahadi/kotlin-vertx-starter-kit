package com.starter.app.domain.auth.db.repository

import com.starter.app.domain.auth.db.model.ModuleRole
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for ModuleRole bean entity
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ModuleRoleRepository @Inject constructor(ebeanServer: Database):
    Repository<ModuleRole, Long>(ebeanServer, ModuleRole::class) {

    suspend fun findByModuleId(moduleId: Long?): Query<ModuleRole> {
        val moduleID = moduleId orDataError "Invalid module ID."
        return query
            .select("*")
            .where()
            .eq("module.id", moduleID)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByRoleId(roleId: Long?): Query<ModuleRole> {
        val roleID = roleId orDataError "Invalid role ID."
        return query
            .select("*")
            .where()
            .eq("role.id", roleID)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByModuleIdAndRoleId(moduleId: Long?, roleId: Long?): Query<ModuleRole> {
        val moduleID = moduleId orDataError "Invalid module ID."
        val roleID = roleId orDataError "Invalid role ID."
        return query
            .select("*")
            .where()
            .and()
                .eq("module.id", moduleID)
                .eq("role.id", roleID)
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByModuleIdAndPermission(
        moduleId: Long?,
        permission: AccessRolePermission? = null,
        keyword: String = ""
    ): Query<ModuleRole> {
        val moduleID = moduleId orDataError "Invalid module ID."
        val q = query
            .select("*")
            .where()
            .and()
                .eq("module.id", moduleID)
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
                    .endAnd()
                    .query()
                    .setAutoTune(false)
            }
    }

    suspend fun findByRoleIdAndPermission(
        roleId: Long?,
        permission: AccessRolePermission? = null,
        keyword: String = ""
    ): Query<ModuleRole> {
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
                if (keyword == "") {
                    this
                } else {
                    this
                        .or()
                            .icontains("module.name", keyword)
                            .icontains("module.code", keyword)
                        .endOr()
                }
            }
            .run {
                this
                    .endAnd()
                    .query()
                    .setAutoTune(false)
            }
    }
}
