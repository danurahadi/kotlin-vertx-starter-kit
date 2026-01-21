package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.model.AccessRole
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import com.starter.app.domain.auth.db.repository.AccessRepository
import com.starter.app.domain.auth.db.repository.AccessRoleRepository
import com.starter.app.domain.auth.db.repository.ModuleRepository
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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [AccessRole] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AccessRoleHandler @Inject constructor(
    private val accessRepository: AccessRepository,
    private val roleRepository: RoleRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val moduleRepository: ModuleRepository,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<AccessRoleHandler>()

    suspend fun getAccessListByRoleId(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val roleId = context.pathParam("id")?.trim() orBadRequest "Role ID path param is required."
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."

        val moduleId = context.param("moduleId")?.trim() orBadRequest "Module ID query param is required."
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

        val module = if (moduleId != "") {
            moduleRepository.findByExternalId(moduleId).findOne() orNotFound "Module data not found."
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
                // create query for get access role list by role ID and permission
                val query = accessRoleRepository.findByRoleIdAndPermission(
                    roleId = role.id,
                    moduleId = module?.id,
                    permission = permission,
                    keyword = keyword
                )

                // execute query as PagedList, so we can get total rows and access role list in paging
                val pagedList = query
                    .orderBy("access.alias ASC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of access roles from the paged list
                val totalData = pagedList.totalCount
                val accessRoles = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/roles/$roleId/access?q=$keyword&permission=$permission&moduleId=$moduleId&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform AccessRole list to DTO class and return them with pagination info as Pair
                accessRoles.map { it.toAccessList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get access role list by role ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get access role list by role ID from DB has been succeed"),
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
                "message" to "Access list by role was successfully fetched."
            )
        }
    }

    suspend fun getRoleListByAccessId(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val accessId = context.pathParam("id")?.trim() orBadRequest "Access ID path param is required."
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
        val access = accessRepository.findByExternalId(accessId).findOne() orNotFound "Access data not found."
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
                // create query for get access role list by access ID and permission
                val query = accessRoleRepository.findByAccessIdAndPermission(
                    accessId = access.id,
                    permission = permission,
                    keyword = keyword
                )

                // execute query as PagedList, so we can get total rows and access role list in paging
                val pagedList = query
                    .orderBy("role.alias ASC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of access roles from the paged list
                val totalData = pagedList.totalCount
                val accessRoles = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/access/$accessId/roles?q=$keyword&permission=$permission&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform AccessRole list to DTO class and return them with pagination info as Pair
                accessRoles.map { it.toRoleList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get access role list by access ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get access role list by access ID from DB has been succeed"),
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
                "message" to "Role list by access was successfully fetched."
            )
        }
    }

    suspend fun getAccessRoleDetails(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val accessId = context.pathParam("accessId").trim() orBadRequest "Access ID path param is required."
        val roleId = context.pathParam("roleId").trim() orBadRequest "Role ID path param is required."

        val access = accessRepository.findByExternalId(accessId).findOne() orNotFound "Access data not found."
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
                // create & execute query for get access role detail by access ID and role ID
                val accessRole = accessRoleRepository.findByAccessAndRole(
                    accessId = access.id,
                    roleId = role.id
                ).findOne() orNotFound "Access role data not found."

                // transform AccessRole entity to DTO class
                accessRole.toAccessRoleDetail()
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get access role detail from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get access role detail from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Access role details was successfully fetched."
            )
        }
    }

    suspend fun updateAccessRolePermission(context: RoutingContext): JsonObject {
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
        val accessId = context.pathParam("accessId").trim() orBadRequest "Access ID path param is required."
        val roleId = context.pathParam("roleId").trim() orBadRequest "Role ID path param is required."

        val access = accessRepository.findByExternalId(accessId).findOne() orNotFound "Access data not found."
        val role = roleRepository.findByExternalId(roleId).findOne() orNotFound "Role data not found."

        val accessRole = accessRoleRepository.findByAccessAndRole(
            accessId = access.id,
            roleId = role.id
        ).findOne() orNotFound "Access role data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val permission = body.get<String>("permission")?.trim() orBadRequest "Permission body param is required."

        val accessRolePermission = try {
            AccessRolePermission.valueOf(permission)
        } catch (_: Exception) {
            throw ValidationException(listOf("Invalid access role permission."))
        }

        // set new value for each AccessRole field
        accessRole.permission = accessRolePermission
        accessRole.lastUpdatedBy = loggedInAdmin
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
                // update AccessRole data to DB
                val accessRoleId = accessRole.id orDataError "Invalid access role data."
                accessRoleRepository.update(code = accessRoleId, o = accessRole)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Update access role permission by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Update access role permission by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to accessRole.toAccessRoleDetail(),
                "message" to "Access role permission was successfully updated."
            )
        }
    }
}
