package com.starter.app.domain.admin.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.admin.adminLog
import com.starter.app.domain.admin.db.model.ActivityLog
import com.starter.app.domain.admin.db.repository.ActivityLogRepository
import com.starter.app.domain.admin.plain.ActivityLogGroup
import com.starter.library.extension.paginate
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.extension.time.toLocalDate
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.orBadRequest
import id.yoframework.web.exception.orDataError
import id.yoframework.web.extension.param
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/***
 * Handler for fetch [ActivityLog] data
 *
 * @author Argi Danu Rahadi
 * @email danu.argi@gmail.com
 *
 **/

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class ActivityLogHandler @Inject constructor(
    private val activityLogRepository: ActivityLogRepository,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<ActivityLogHandler>()

    suspend fun getActivityLogs(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity)
        val userTimezone = loggedInUser.setting.timezoneOffset
        /**
         * End of authentication process
         *
         */

        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val startDateStr = context.param("startDate")?.trim() orBadRequest "Start date query param is required."
        val endDateStr = context.param("endDate")?.trim() orBadRequest "End date query param is required."

        val page = context.param("page")?.trim()?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.trim()?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val startDate = if (startDateStr != "") {
            try {
                startDateStr.toLocalDate("yyyy-MM-dd")
            } catch (_: Exception) {
                throw BadRequestException("Invalid start date query param.")
            }
        } else null

        val endDate = if (endDateStr != "") {
            try {
                endDateStr.toLocalDate("yyyy-MM-dd")
            } catch (_: Exception) {
                throw BadRequestException("Invalid end date query param.")
            }
        } else null

        val startDateTime = if (startDate != null) {
            LocalDateTime.of(startDate, LocalTime.of(0,0,0)).minusMinutes(userTimezone)
        } else null

        val endDateTime = if (endDate != null) {
            LocalDateTime.of(endDate, LocalTime.of(23,59,59)).minusMinutes(userTimezone)
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
                // create query for get activity log list
                val query = activityLogRepository.findByDateTimeAndDescriptionContain(
                    startDateTime = startDateTime,
                    endDateTime = endDateTime,
                    keyword = keyword
                )

                // execute query against DB and retrieve the results in paging
                val pagedList = query
                    .orderBy("createdAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get activity log list
                val activityLogs = pagedList.list

                // get total rows from the future
                val totalData = pagedList.totalCount

                val paginationInfo = paginate(
                    "$apiBaseUrl/activity-logs?q=$keyword&startDate=$startDateStr&endDate=$endDateStr&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform ActivityLog list to DTO class and return them with Pagination info as Pair
                activityLogs.groupBy { al ->
                    val createdAt = al.createdAt.toLocalDate()
                    createdAt
                }
                    .map { l ->
                        val date = l.key.toString()
                        val logs = l.value.map { it.toActivityLogList(userTimezone) }

                        ActivityLogGroup(
                            date = date,
                            logs = logs
                        )
                    } to paginationInfo
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("Get activity logs from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("Get activity logs from DB has been succeed"),
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
                "message" to "Activity logs was successfully fetched."
            )
        }
    }
}
