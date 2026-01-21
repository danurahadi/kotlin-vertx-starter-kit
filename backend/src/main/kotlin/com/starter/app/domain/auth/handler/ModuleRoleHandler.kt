package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.model.ModuleRole
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import com.starter.app.domain.auth.db.repository.AccessRoleRepository
import com.starter.app.domain.auth.db.repository.ModuleRepository
import com.starter.app.domain.auth.db.repository.ModuleRoleRepository
import com.starter.app.domain.auth.db.repository.RoleRepository
import com.starter.library.extension.paginate
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.web.exception.*
import id.yoframework.web.extension.jsonBody
import id.yoframework.web.extension.param
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [ModuleRole] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ModuleRoleHandler @Inject constructor(
    private val moduleRepository: ModuleRepository,
    private val roleRepository: RoleRepository,
    private val moduleRoleRepository: ModuleRoleRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<ModuleRoleHandler>()

    suspend fun getModuleListByRoleId(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val roleId = context.pathParam("id")?.trim() orBadRequest "Role ID path param is required."
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val permissionStr = context.param("permission")?.trim() orBadRequest "Permission query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val permission = if (permissionStr != "") {
            try {
                AccessRolePermission.valueOf(permissionStr)
            } catch (_: Exception) {
                throw BadRequestException("Invalid permission query param.")
            }
        } else null
        val role = roleRepository.findByExternalId(roleId).findOne() orNotFound "Role data not found."
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create query for get module role list by role ID and permission
                val query = moduleRoleRepository.findByRoleIdAndPermission(
                    roleId = role.id,
                    permission = permission,
                    keyword = keyword
                )

                // execute query as PagedList, so we can get total rows and module role list in paging
                val pagedList = query
                    .orderBy("module.name ASC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of module roles from the paged list
                val totalData = pagedList.totalCount
                val moduleRoles = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/roles/$roleId/modules?q=$keyword&permission=$permission&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform ModuleRole list to DTO class and return them with pagination info as Pair
                moduleRoles.map { it.toModuleList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get module role list by role ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get module role list by role ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data.first,
                "pagination" to data.second,
                "message" to "Module list by role was successfully fetched."
            )
        }
    }

    suspend fun getRoleListByModuleId(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val moduleId = context.pathParam("id")?.trim() orBadRequest "Module ID path param is required."
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val permissionStr = context.param("permission")?.trim() orBadRequest "Permission query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val permission = if (permissionStr != "") {
            try {
                AccessRolePermission.valueOf(permissionStr)
            } catch (_: Exception) {
                throw BadRequestException("Invalid permission query param.")
            }
        } else null
        val module = moduleRepository.findByExternalId(moduleId).findOne() orNotFound "Module data not found."
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create query for get module role list by module ID and permission
                val query = moduleRoleRepository.findByModuleIdAndPermission(
                    moduleId = module.id,
                    permission = permission,
                    keyword = keyword
                )

                // execute query as PagedList, so we can get total rows and module role list in paging
                val pagedList = query
                    .orderBy("role.alias ASC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of access roles from the paged list
                val totalData = pagedList.totalCount
                val moduleRoles = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/modules/$moduleId/roles?q=$keyword&permission=$permission&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform AccessRole list to DTO class and return them with pagination info as Pair
                moduleRoles.map { it.toRoleList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get module role list by module ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get module role list by module ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data.first,
                "pagination" to data.second,
                "message" to "Role list by module was successfully fetched."
            )
        }
    }

    suspend fun getModuleRoleDetails(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val moduleId = context.pathParam("moduleId")?.trim() orBadRequest "Module ID path param is required."
        val roleId = context.pathParam("roleId")?.trim() orBadRequest "Role ID path param is required."

        val module = moduleRepository.findByExternalId(moduleId).findOne() orNotFound "Module data not found."
        val role = roleRepository.findByExternalId(roleId).findOne() orNotFound "Role data not found."
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get module role detail by module ID and role ID
                val moduleRole = moduleRoleRepository.findByModuleIdAndRoleId(
                    moduleId = module.id,
                    roleId = role.id
                ).findOne() orNotFound "Module role data not found."

                // transform ModuleRole entity to DTO class
                moduleRole.toModuleRoleDetail()
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get module role detail from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get module role detail from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Module role details was successfully fetched."
            )
        }
    }

    suspend fun updateModuleRolePermission(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity)
        /**
         * End of authentication process
         *
         */

        /**
         * Validation process
         *
         */
        val moduleId = context.pathParam("moduleId")?.trim() orBadRequest "Module ID path param is required."
        val roleId = context.pathParam("roleId")?.trim() orBadRequest "Role ID path param is required."

        val module = moduleRepository.findByExternalId(moduleId).findOne() orNotFound "Module data not found."
        val role = roleRepository.findByExternalId(roleId).findOne() orNotFound "Role data not found."

        val moduleRole = moduleRoleRepository.findByModuleIdAndRoleId(
            moduleId = module.id,
            roleId = role.id
        ).findOne() orNotFound "Module role data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val permissionStr = body.get<String>("permission")?.trim() orBadRequest "Permission body param is required."

        val permission = try {
            AccessRolePermission.valueOf(permissionStr)
        } catch (_: Exception) {
            throw ValidationException(listOf("Invalid access role permission."))
        }

        // set new value for each ModuleRole field
        moduleRole.permission = permission
        moduleRole.lastUpdatedBy = loggedInAdmin
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // update ModuleRole data to DB
                val moduleRoleId = moduleRole.id orDataError "Invalid module role data."
                moduleRoleRepository.update(code = moduleRoleId, o = moduleRole)

                // update all related access roles permission
                CoroutineScope(Dispatchers.IO).launch {
                    val accessRoles = accessRoleRepository.findByModuleIdAndRoleId(
                        moduleId = module.id,
                        roleId = role.id
                    ).findList()

                    val updatedAccessRoles = accessRoles.map { ar ->
                        ar.permission = permission
                        ar
                    }
                    accessRoleRepository.updateAll(updatedAccessRoles)
                }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Update module role permission by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Update module role permission by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to moduleRole.toModuleRoleDetail(),
                "message" to "Module role permission was successfully updated."
            )
        }
    }
}
