package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.model.Module
import com.starter.app.domain.auth.db.model.ModuleRole
import com.starter.app.domain.auth.db.repository.ModuleRepository
import com.starter.app.domain.auth.db.repository.ModuleRoleRepository
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
 * Handler class for manage [Module] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ModuleHandler @Inject constructor(
    private val moduleRepository: ModuleRepository,
    private val roleRepository: RoleRepository,
    private val moduleRoleRepository: ModuleRoleRepository,
    private val authorizationService: AuthorizationService,
    private val ebeanServer: Database,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<ModuleHandler>()

    suspend fun createModule(context: RoutingContext): JsonObject {
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
        val code = body.get<String>("code")?.trim() orBadRequest "Code body param is required."

        val name = body.get<String>("name")?.trim() orBadRequest "Name body param is required."
        val summary = body.get<String>("summary")?.trim() orBadRequest "Summary body param is required."
        
        // instantiate object from module entity
        val module = Module(
            code = code,
            name = name,
            summary = summary,
            createdBy = loggedInAdmin,
            lastUpdatedBy = loggedInAdmin
        )

        // validate module entity
        module.validate()
        module.validateUnique(ebeanServer)
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

                    // insert module data to DB
                    moduleRepository.insert(module)

                    // setup module roles data
                    val roles = roleRepository.findAll()
                    val moduleRoles = roles.map { r ->
                        ModuleRole(
                            module = module,
                            role = r
                        )
                    }

                    // insert module roles data
                    moduleRoleRepository.insertAll(moduleRoles)

                    // commit DB transaction
                    it.commit()
                }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Create module has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Create module has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to module.toModuleDetail(),
                "message" to "Module was successfully created."
            )
        }
    }

    suspend fun getModuleList(context: RoutingContext): JsonObject {
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
                // create query for get module list by code or name containing
                val query = moduleRepository.findByCodeOrNameContain(keyword)

                // execute query as PagedList, so we can get total rows and module list in paging
                val pagedList = query
                    .orderBy("lastUpdatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of modules from the paged list
                val totalData = pagedList.totalCount
                val modules = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/modules?q=$keyword&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform Module list to DTO class and return them with pagination info as Pair
                modules.map { it.toModuleList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get module list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get module list from DB has been succeed"),
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
                "message" to "Module list was successfully fetched."
            )
        }
    }

    fun getAllModules(): JsonObject {
        /**
         * Core process
         *
         */
        val (module, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get all modules
                val modules = moduleRepository.findAll()

                // transform Module entity to DTO class
                modules.map { it.toModuleCompact() }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get all modules from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get all modules from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to module,
                "message" to "All modules was successfully fetched."
            )
        }
    }

    suspend fun getModuleDetails(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim() orBadRequest "Invalid module ID path param."
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (module, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get module by external ID
                val module = moduleRepository.findByExternalId(id).findOne() orNotFound "Module data not found."

                // transform Module entity to DTO class
                module.toModuleDetail()
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get module detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get module detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to module,
                "message" to "Module details was successfully fetched."
            )
        }
    }

    suspend fun updateModule(context: RoutingContext): JsonObject {
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
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid module ID path param."
        val module = moduleRepository.findByExternalId(externalId).findOne() orNotFound "Module data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val code = body.get<String>("code")?.trim() orBadRequest "Code body param is required."

        val name = body.get<String>("name")?.trim() orBadRequest "Name body param is required."
        val summary = body.get<String>("summary")?.trim() orBadRequest "Summary body param is required."

        // set new value for each Module field
        module.code = code
        module.name = name

        module.summary = summary
        module.lastUpdatedBy = loggedInAdmin

        // validate module entity
        module.validate()
        module.validateUnique(ebeanServer)
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
                // update Module data to DB
                val id = module.id orDataError "Invalid module data."
                moduleRepository.update(code = id, o = module)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Update module by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Update module by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to module.toModuleDetail(),
                "message" to "Module was successfully updated."
            )
        }
    }

    suspend fun deleteModule(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid module ID path param."
        val module = moduleRepository.findByExternalId(externalId).findOne() orNotFound "Module data not found."
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

                // execute query in transaction and auto closes it in the end
                transaction.use {

                    // delete related module roles data
                    val relatedModuleRoles = moduleRoleRepository.findByModuleId(module.id).findList()
                    moduleRoleRepository.deleteAll(relatedModuleRoles)

                    // delete Module data from DB
                    moduleRepository.delete(module)

                    // commit DB transaction
                    transaction.commit()
                }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Delete module by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Delete module by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Module was successfully deleted."
            )
        }
    }
}
