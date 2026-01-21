package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.model.AccessRole
import com.starter.app.domain.auth.db.model.ModuleRole
import com.starter.app.domain.auth.db.model.Role
import com.starter.app.domain.auth.db.repository.*
import com.starter.app.domain.user.db.repository.CmsUserRepository
import com.starter.library.extension.paginate
import com.starter.library.extension.validate
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
 * Handler class for manage [Role] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class RoleHandler @Inject constructor(
    private val roleRepository: RoleRepository,
    private val accessRepository: AccessRepository,
    private val moduleRepository: ModuleRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val moduleRoleRepository: ModuleRoleRepository,
    private val cmsUserRepository: CmsUserRepository,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<RoleHandler>()

    suspend fun createRole(context: RoutingContext): JsonObject {
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
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val name = body.get<String>("name")?.trim() orBadRequest "Name body param is required."

        val alias = body.get<String>("alias")?.trim() orBadRequest "Alias body param is required."
        val description = body.get<String>("description")?.trim()

        // instantiate object from role entity
        val role = Role(
            name = name,
            alias = alias,
            description = description,
            createdBy = loggedInAdmin,
            lastUpdatedBy = loggedInAdmin
        )

        // validate role object
        role.validate()

        // unique validation for role name
        val existingName = roleRepository.findByName(name).findCount()
        if (existingName > 0) {
            throw ValidationException(listOf("Role '$name' already exist. Try another."))
        }
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
                // insert role data to DB
                roleRepository.insert(role)

                CoroutineScope(Dispatchers.IO).launch {

                    // setup access roles data
                    val access = accessRepository.findAll()
                    val accessRoles = access.map { a ->
                        AccessRole(
                            access = a,
                            role = role,
                            createdBy = loggedInAdmin,
                            lastUpdatedBy = loggedInAdmin
                        )
                    }

                    // setup module roles data
                    val modules = moduleRepository.findAll()
                    val moduleRoles = modules.map { m ->
                        ModuleRole(
                            module = m,
                            role = role
                        )
                    }

                    // insert access roles data
                    accessRoleRepository.insertAll(accessRoles)

                    // insert module roles data
                    moduleRoleRepository.insertAll(moduleRoles)
                }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Create role has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Create role has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to role.toRoleDetail(),
                "message" to "Role was successfully created."
            )
        }
    }

    suspend fun getRoleList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit
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
                // create query for get role list by alias containing
                val query = roleRepository.findByNameOrAliasContain(keyword)

                // execute query as PagedList, so we can get total rows and role list in paging
                val pagedList = query
                    .orderBy("lastUpdatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of roles from the paged list
                val totalData = pagedList.totalCount
                val roles = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/roles?q=$keyword&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform Role list to DTO class and return them with pagination info as Pair
                roles.map { it.toRoleList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get role list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get role list from DB has been succeed"),
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
                "message" to "Role list was successfully fetched."
            )
        }
    }

    suspend fun getAutocompleteRoles(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
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
                // create & execute query for get role list by alias containing
                val roles = roleRepository.findByNameOrAliasContain(keyword)
                    .setFirstRow(0)
                    .setMaxRows(15)
                    .findList()

                // transform Role list to DTO class
                roles.map { it.toRoleList() }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get autocomplete roles from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get autocomplete roles from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Autocomplete roles was successfully fetched."
            )
        }
    }

    suspend fun getRoleDetails(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim() orBadRequest "Invalid role ID path param."
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
                // create & execute query for get role by external ID
                val role = roleRepository.findByExternalId(id).findOne() orNotFound "Role data not found."

                // transform Role entity to DTO class
                role.toRoleDetail()
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get role detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get role detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Role details was successfully fetched."
            )
        }
    }

    suspend fun updateRole(context: RoutingContext): JsonObject {
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
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid role ID path param."
        val role = roleRepository.findByExternalId(externalId).findOne() orNotFound "Role data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val alias = body.get<String>("alias")?.trim() orBadRequest "Alias body param is required."
        val description = body.get<String>("description")?.trim()

        // set new value for each Role field
        role.alias = alias
        role.description = description
        role.lastUpdatedBy = loggedInAdmin

        // validate role entity
        role.validate()
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
                // update Role data to DB
                val id = role.id orDataError "Invalid role data."
                roleRepository.update(code = id, o = role)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Update role by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Update role by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to role.toRoleDetail(),
                "message" to "Role was successfully updated."
            )
        }
    }

    suspend fun deleteRole(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid role ID path param."
        val role = roleRepository.findByExternalId(externalId).findOne() orNotFound "Role data not found."

        val relatedUsersCount = cmsUserRepository.findByRoleId(role.id).findCount()
        if (relatedUsersCount > 0) throw ValidationException(listOf("Role could not be deleted because there is related users data."))
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
                // delete related access roles data
                val relatedAccessRoles = accessRoleRepository.findByRoleId(role.id).findList()
                accessRoleRepository.deleteAll(relatedAccessRoles)

                // delete related module roles data
                val relatedModuleRoles = moduleRoleRepository.findByRoleId(role.id).findList()
                moduleRoleRepository.deleteAll(relatedModuleRoles)

                // delete Role data from DB
                roleRepository.delete(role)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Delete role by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Delete role by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Role was successfully deleted."
            )
        }
    }
}
