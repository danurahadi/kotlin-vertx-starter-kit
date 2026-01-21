package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.model.Access
import com.starter.app.domain.auth.db.model.AccessRole
import com.starter.app.domain.auth.db.repository.AccessRepository
import com.starter.app.domain.auth.db.repository.AccessRoleRepository
import com.starter.app.domain.auth.db.repository.ModuleRepository
import com.starter.app.domain.auth.db.repository.RoleRepository
import com.starter.library.extension.paginate
import com.starter.library.extension.validate
import com.starter.library.extension.validateUnique
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.orBadRequest
import id.yoframework.web.exception.orDataError
import id.yoframework.web.exception.orNotFound
import id.yoframework.web.extension.jsonBody
import id.yoframework.web.extension.param
import io.ebean.Database
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [Access] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AccessHandler @Inject constructor(
    private val accessRepository: AccessRepository,
    private val roleRepository: RoleRepository,
    private val moduleRepository: ModuleRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val ebeanServer: Database,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<AccessHandler>()

    suspend fun createAccess(context: RoutingContext): JsonObject {
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
        val moduleId = body.get<String>("moduleId")?.trim()

        val name = body.get<String>("name")?.trim() orBadRequest "Name body param is required."
        val alias = body.get<String>("alias")?.trim() orBadRequest "Alias body param is required."

        val module = if (moduleId != null) {
            moduleRepository.findByExternalId(moduleId)
                .findOne() orNotFound "Module data not found."
        } else null

        // instantiate object from access entity
        val access = Access(
            module = module,
            name = name,
            alias = alias,
            createdBy = loggedInAdmin,
            lastUpdatedBy = loggedInAdmin
        )

        // validate access entity
        access.validate()
        access.validateUnique(ebeanServer)
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
                // start DB transaction
                val transaction = ebeanServer.beginTransaction()

                transaction.use {
                    // insert access data to DB
                    accessRepository.insert(access)

                    // setup access roles data
                    val roles = roleRepository.findAll()
                    val accessRoles = roles.map { r ->
                        AccessRole(
                            access = access,
                            role = r,
                            createdBy = loggedInAdmin,
                            lastUpdatedBy = loggedInAdmin
                        )
                    }

                    // insert access roles data
                    accessRoleRepository.insertAll(accessRoles)

                    // commit DB transaction
                    it.commit()
                }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Create access has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Create access has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to access.toAccessDetail(),
                "message" to "Access was successfully created."
            )
        }
    }

    suspend fun getAccessList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val moduleId = context.param("moduleId")?.trim() orBadRequest "Module ID query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val module = if (moduleId != "") {
            moduleRepository.findByExternalId(moduleId).findOne() orNotFound "Module data not found."
        } else null
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
                // create query for get access list by alias containing
                val query = accessRepository.findByNameOrAliasContain(
                    moduleId = module?.id,
                    keyword = keyword,
                )

                // execute query as PagedList, so we can get total rows and access list in paging
                val pagedList = query
                    .orderBy("lastUpdatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of access from the paged list
                val totalData = pagedList.totalCount
                val access = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/access?q=$keyword&moduleId=$moduleId&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform Access list to DTO class and return them with pagination info as Pair
                access.map { it.toAccessList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get access list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get access list from DB has been succeed"),
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
                "message" to "Access list was successfully fetched."
            )
        }
    }

    suspend fun getAutocompleteAccess(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val moduleId = context.param("moduleId")?.trim() orBadRequest "Module ID query param is required."

        val module = if (moduleId != "") {
            moduleRepository.findByExternalId(moduleId).findOne() orNotFound "Module data not found."
        } else null
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
                // create query for get access list by alias containing
                val query = accessRepository.findByNameOrAliasContain(
                    moduleId = module?.id,
                    keyword = keyword
                )

                // execute query as List
                val access = query
                    .setFirstRow(0)
                    .setMaxRows(15)
                    .findList()

                // transform Access list to DTO class
                access.map { it.toAccessList() }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get autocomplete access from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get autocomplete access from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Autocomplete access was successfully fetched."
            )
        }
    }

    suspend fun getAccessDetails(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id").trim() orBadRequest "Invalid access ID path param."
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
                // create & execute query for get access by external ID
                val access = accessRepository.findByExternalId(id).findOne() orNotFound "Access data not found."

                // transform Access entity to DTO class
                access.toAccessDetail()
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get access detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get access detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Access details was successfully fetched."
            )
        }
    }

    suspend fun updateAccess(context: RoutingContext): JsonObject {
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
        val externalId = context.pathParam("id").trim() orBadRequest "Invalid access ID path param."
        val access = accessRepository.findByExternalId(externalId).findOne() orNotFound "Access data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val moduleId = body.get<String>("moduleId")?.trim()
        val alias = body.get<String>("alias")?.trim() orBadRequest "Alias body param is required."

        val module = if (moduleId != null) {
            moduleRepository.findByExternalId(moduleId)
                .findOne() orNotFound "Module data not found."
        } else null

        // set new value for each Access field
        access.module = module
        access.alias = alias
        access.lastUpdatedBy = loggedInAdmin

        // validate access entity
        access.validate()
        access.validateUnique(ebeanServer)
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
                // update Access data to DB
                val id = access.id orDataError "Invalid access data."
                accessRepository.update(code = id, o = access)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Update access by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Update access by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to access.toAccessDetail(),
                "message" to "Access was successfully updated."
            )
        }
    }

    suspend fun deleteAccess(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id").trim() orBadRequest "Invalid access ID path param."
        val access = accessRepository.findByExternalId(externalId).findOne() orNotFound "Access data not found."
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
                val relatedAccessRoles = accessRoleRepository.findByAccessId(access.id).findList()
                accessRoleRepository.deleteAll(relatedAccessRoles)

                // delete Access data from DB
                accessRepository.delete(access)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Delete access by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Delete access by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Access was successfully deleted."
            )
        }
    }
}
