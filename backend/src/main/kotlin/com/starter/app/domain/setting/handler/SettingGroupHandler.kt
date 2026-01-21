package com.starter.app.domain.setting.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.setting.db.model.AppSetting
import com.starter.app.domain.setting.db.model.SettingGroup
import com.starter.app.domain.setting.db.model.value.SettingType
import com.starter.app.domain.setting.db.repository.AppSettingRepository
import com.starter.app.domain.setting.db.repository.SettingGroupRepository
import com.starter.app.domain.setting.settingLog
import com.starter.library.extension.paginate
import com.starter.library.extension.validate
import com.starter.library.extension.validateUnique
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.web.exception.*
import id.yoframework.web.extension.jsonBody
import id.yoframework.web.extension.param
import io.ebean.Database
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [SettingGroup] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class SettingGroupHandler @Inject constructor(
    private val settingGroupRepository: SettingGroupRepository,
    private val appSettingRepository: AppSettingRepository,
    private val authorizationService: AuthorizationService,
    private val ebeanServer: Database,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<SettingGroupHandler>()

    suspend fun createSettingGroup(context: RoutingContext): JsonObject {
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
        
        val settingKey = body.get<String>("settingKey")?.trim() orBadRequest "Setting key body param is required."
        val settingValue = body.get<String>("settingValue")?.trim() orBadRequest "Setting value body param is required."

        val settingTypeStr = body.get<String>("settingType")?.trim() orBadRequest "Setting type body param is required."
        val settingOptions = body.get<JsonArray>("settingOptions")

        val settingType = try {
            SettingType.valueOf(settingTypeStr)
        } catch (_: Exception) {
            throw ValidationException(listOf("Invalid setting type body param."))
        }
        val settingOptionsList = settingOptions?.map { it.toString() }

        // instantiate object from SettingGroup entity
        val settingGroup = SettingGroup(
            name = name,
            createdBy = loggedInAdmin,
            lastUpdatedBy = loggedInAdmin
        )

        // validate setting group object
        settingGroup.validate()
        settingGroup.validateUnique(ebeanServer)
        
        // instantiate object from AppSetting entity
        val appSetting = AppSetting(
            settingGroup = settingGroup,
            settingKey = settingKey,
            settingValue = settingValue,
            settingType = settingType,
            settingOptions = settingOptionsList,
            createdBy = loggedInAdmin,
            lastUpdatedBy = loggedInAdmin
        )

        // validate app setting object
        appSetting.validate()
        appSetting.validateUnique(ebeanServer)
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
                    // insert setting group data to DB
                    settingGroupRepository.insert(settingGroup)
                    
                    // insert app setting data to DB
                    appSettingRepository.insert(appSetting)

                    // commit DB transaction
                    it.commit()
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Create setting group has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Create setting group has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to settingGroup.toSettingGroupDetail(),
                "message" to "Setting group was successfully created."
            )
        }
    }

    suspend fun getAllSettingGroups(): JsonObject {
        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get all setting groups
                val settingGroups = settingGroupRepository.findByNameContain(keyword = "")
                    .orderBy("name ASC")
                    .findList()

                // transform SettingGroup list to DTO class
                settingGroups.map { it.toSettingGroupList() }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get all setting groups from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get all setting groups from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "All setting groups was successfully fetched."
            )
        }
    }

    suspend fun getAllSettingGroupsWithAppSettings(): JsonObject {
        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get all setting groups
                val settingGroups = settingGroupRepository.findByNameContain(keyword = "")
                    .orderBy("name ASC")
                    .findList()

                // transform SettingGroup list to DTO class
                settingGroups.map { it.toSettingGroupCompact() }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get all setting groups with app settings from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get all setting groups with app settings from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "All app settings was successfully fetched."
            )
        }
    }

    suspend fun getSettingGroupList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 0) throw BadRequestException("Invalid pagination query params.")
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
                // create query for get setting group list by name containing
                val query = settingGroupRepository.findByNameContain(keyword)

                // execute query as PagedList so we can get total rows and setting group list in paging
                val pagedList = query
                    .orderBy("lastUpdatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of setting groups from the paged list
                val totalData = pagedList.totalCount
                val settingGroups = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/setting-groups?q=$keyword&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform SettingGroup list to DTO class and return them with pagination info as Pair
                settingGroups.map { it.toSettingGroupList() } to paginationInfo
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get setting group list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get setting group list from DB has been succeed"),
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
                "message" to "Setting group list was successfully fetched."
            )
        }
    }

    suspend fun getSettingGroupDetails(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim() orBadRequest "Invalid setting group ID path param."
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
                // create & execute query for get setting group by external ID
                val settingGroup = settingGroupRepository.findByExternalId(id)
                    .findOne() orNotFound "Setting group data not found."

                // transform SettingGroup entity to DTO class
                settingGroup.toSettingGroupDetail()
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get setting group detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get setting group detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Setting group details was successfully fetched."
            )
        }
    }

    suspend fun updateSettingGroup(context: RoutingContext): JsonObject {
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
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid setting group ID path param."
        val settingGroup = settingGroupRepository.findByExternalId(externalId)
            .findOne() orNotFound "Setting group data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val name = body.get<String>("name")?.trim() orBadRequest "Name body param is required."

        // set new value for each SettingGroup field
        settingGroup.name = name
        settingGroup.lastUpdatedBy = loggedInAdmin

        // validate settingGroup entity
        settingGroup.validate()
        settingGroup.validateUnique(ebeanServer)
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
                // update Setting Group data to DB
                val id = settingGroup.id orDataError "Invalid setting group data."
                settingGroupRepository.update(code = id, o = settingGroup)
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Update setting group by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Update setting group by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to settingGroup.toSettingGroupDetail(),
                "message" to "Setting group was successfully updated."
            )
        }
    }

    suspend fun deleteSettingGroup(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid setting group ID path param."
        val settingGroup = settingGroupRepository.findByExternalId(externalId)
            .findOne() orNotFound "Setting group data not found."
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
                // delete SettingGroup data from DB
                settingGroupRepository.delete(settingGroup)
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Delete setting group by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Delete setting group by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Setting group was successfully deleted."
            )
        }
    }
}
