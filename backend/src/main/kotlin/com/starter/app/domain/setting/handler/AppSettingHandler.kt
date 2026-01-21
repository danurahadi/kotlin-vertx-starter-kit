package com.starter.app.domain.setting.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.setting.db.model.AppSetting
import com.starter.app.domain.setting.db.model.value.SettingType
import com.starter.app.domain.setting.db.repository.AppSettingRepository
import com.starter.app.domain.setting.db.repository.SettingGroupRepository
import com.starter.app.domain.setting.plain.AppSettingValue
import com.starter.app.domain.setting.settingLog
import com.starter.library.extension.paginate
import com.starter.library.extension.validate
import com.starter.library.extension.validateUnique
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.ValidationException
import id.yoframework.web.exception.orBadRequest
import id.yoframework.web.exception.orDataError
import id.yoframework.web.exception.orNotFound
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
 * Handler class for manage [AppSetting] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AppSettingHandler @Inject constructor(
    private val appSettingRepository: AppSettingRepository,
    private val settingGroupRepository: SettingGroupRepository,
    private val authorizationService: AuthorizationService,
    private val ebeanServer: Database,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<AppSettingHandler>()

    suspend fun createAppSetting(context: RoutingContext): JsonObject {
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
        val settingGroupId = body.get<String>("settingGroupId")
            ?.trim() orBadRequest "Setting group ID body param is required."

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
        val settingGroup = settingGroupRepository.findByExternalId(settingGroupId)
            .findOne() orNotFound "Setting group data not found."

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
                // insert app setting data to DB
                appSettingRepository.insert(appSetting)
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Create app setting has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Create app setting has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to appSetting.toAppSettingDetail(),
                "message" to "App setting was successfully created."
            )
        }
    }

    suspend fun getAppSettingList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val settingGroupId = context.param("settingGroupId")
            ?.trim() orBadRequest "Setting group ID query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 0) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val settingGroup = if (settingGroupId == "") {
            null
        } else {
            settingGroupRepository.findByExternalId(settingGroupId).findOne() orNotFound "Setting group data not found."
        }
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
                // create query for get app setting list by name containing
                val query = appSettingRepository.findBySettingGroupIdAndKeyContain(
                    settingGroupId = settingGroup?.id,
                    keyword = keyword
                )

                // execute query as PagedList so we can get total rows and app setting list in paging
                val pagedList = query
                    .orderBy("lastUpdatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of app settings from the paged list
                val totalData = pagedList.totalCount
                val appSettings = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/app-settings?q=$keyword&settingGroupId=$settingGroupId&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform AppSetting list to DTO class and return them with pagination info as Pair
                appSettings.map { it.toAppSettingList() } to paginationInfo
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get app setting list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get app setting list from DB has been succeed"),
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
                "message" to "App setting list was successfully fetched."
            )
        }
    }

    suspend fun getAppSettingDetailsByIdOrKey(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim() orBadRequest "Invalid app setting ID path param."
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
                // create & execute query for get app setting by external ID or setting key
                val appSetting = appSettingRepository.findByExternalIdOrKey(id)
                    .findOne() orNotFound "App setting data not found."

                // transform AppSetting entity to DTO class
                appSetting.toAppSettingDetail()
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get app setting detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get app setting detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "App setting details was successfully fetched."
            )
        }
    }

    suspend fun bulkUpdateAppSettings(context: RoutingContext): JsonObject {
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
        val appSettings = body.get<JsonArray>("appSettings") orBadRequest "App settings body param is required."

        val appSettingValues = appSettings.map { s ->
            val setting = s as JsonObject

            val id = setting.get<String>("id") orBadRequest "App setting ID is required."
            val settingValue = setting.get<String>("settingValue") orBadRequest "App setting value is required."

            AppSettingValue(
                id = id,
                settingValue = settingValue
            )
        }

        val existingAppSettings = appSettingRepository.findAll()
        val updatedAppSettings = existingAppSettings.map { s ->
            val matchAppSetting = appSettingValues.find {
                it.id == s.externalId
            } orNotFound "App setting data not found."

            val currentValue = s.settingValue
            val newValue = matchAppSetting.settingValue

            if (currentValue != newValue) {
                s.settingValue = newValue
                s.lastUpdatedBy = loggedInAdmin
            }

            s.validate()
            s
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
                // bulk update App Setting data to DB
                appSettingRepository.updateAll(list = updatedAppSettings)
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Bulk update app settings has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Bulk update app settings has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "App settings was successfully updated."
            )
        }
    }

    suspend fun updateAppSetting(context: RoutingContext): JsonObject {
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
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid app setting ID path param."
        val appSetting = appSettingRepository.findByExternalId(externalId)
            .findOne() orNotFound "App setting data not found."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val settingGroupId = body.get<String>("settingGroupId")
            ?.trim() orBadRequest "Setting group ID body param is required."

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
        val settingGroup = settingGroupRepository.findByExternalId(settingGroupId)
            .findOne() orNotFound "Setting group data not found."

        // set new value for each AppSetting field
        appSetting.settingGroup = settingGroup
        appSetting.settingKey = settingKey
        appSetting.settingValue = settingValue

        appSetting.settingType = settingType
        appSetting.settingOptions = settingOptionsList
        appSetting.lastUpdatedBy = loggedInAdmin

        // validate appSetting entity
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
                // update App Setting data to DB
                val id = appSetting.id orDataError "Invalid app setting data."
                appSettingRepository.update(code = id, o = appSetting)
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Update app setting by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Update app setting by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to appSetting.toAppSettingDetail(),
                "message" to "App setting was successfully updated."
            )
        }
    }

    suspend fun deleteAppSetting(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid app setting ID path param."
        val appSetting = appSettingRepository.findByExternalId(externalId)
            .findOne() orNotFound "App setting data not found."
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
                // delete AppSetting data from DB
                appSettingRepository.delete(appSetting)
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Delete app setting by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Delete app setting by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "App setting was successfully deleted."
            )
        }
    }
}
