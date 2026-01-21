package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.model.ApiKey
import com.starter.app.domain.auth.db.repository.ApiKeyRepository
import com.starter.library.extension.generateSecureRandomChars
import com.starter.library.extension.paginate
import com.starter.library.extension.validate
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
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [ApiKey] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ApiKeyHandler @Inject constructor(
    private val apiKeyRepository: ApiKeyRepository,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<ApiKeyHandler>()

    suspend fun createApiKey(context: RoutingContext): JsonObject {
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
        val title = body.get<String>("title")?.trim() orBadRequest "Name body param is required."

        // instantiate object from API Key entity
        val apiKey = ApiKey(
            cmsUser = null,
            title = title,
            ipAddress = "0.0.0.0",
            location = "",
            accessToken = generateSecureRandomChars(32),
            expiredTime = LocalDateTime.now().plusYears(50),
            createdBy = loggedInAdmin
        )

        // validate API Key entity
        apiKey.validate()

        val existingTitle = apiKeyRepository.findByTitle(title).findCount()
        if (existingTitle > 0) {
            throw ValidationException(listOf("Title already exist. Try another."))
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
                // insert API key data to DB
                apiKeyRepository.insert(apiKey)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Generate API key has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Generate API key has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to apiKey.toApiKeyList(),
                "message" to "API key was successfully generated."
            )
        }
    }

    suspend fun getApiKeyList(context: RoutingContext): JsonObject {
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
                // create query for get API key list by title containing
                val query = apiKeyRepository.findByTitleContaining(keyword)

                // execute query as PagedList, so we can get total rows and API key list in paging
                val pagedList = query
                    .orderBy("createdAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get total data and list of API key from the paged list
                val totalData = pagedList.totalCount
                val apiKeys = pagedList.list

                // setup pagination object
                val paginationInfo = paginate(
                    "$apiBaseUrl/api-keys?q=$keyword&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform API Key list to DTO class and return them with pagination info as Pair
                apiKeys.map { it.toApiKeyList() } to paginationInfo
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Get API key list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Get API key list from DB has been succeed"),
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
                "message" to "API key list was successfully fetched."
            )
        }
    }

    suspend fun deleteApiKey(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid API key ID path param."
        val apiKey = apiKeyRepository.findByExternalId(externalId).findOne() orNotFound "API key data not found."
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
                // delete API key data from DB
                apiKeyRepository.delete(apiKey)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Delete API key by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Delete API key by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "API key was successfully deleted."
            )
        }
    }
}
