package com.starter.app.domain.notification.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.notification.db.model.AdminNotification
import com.starter.app.domain.notification.db.model.value.NotificationStatus
import com.starter.app.domain.notification.db.repository.AdminNotificationRepository
import com.starter.app.domain.notification.notifLog
import com.starter.app.domain.notification.plain.UnreadNotification
import com.starter.app.integration.eventbus.EventBusService
import com.starter.library.extension.paginate
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.toJson
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.InvalidCredentials
import id.yoframework.web.exception.orBadRequest
import id.yoframework.web.exception.orDataError
import id.yoframework.web.exception.orNotFound
import id.yoframework.web.extension.param
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [AdminNotification] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AdminNotificationHandler @Inject constructor(
    private val adminNotificationRepository: AdminNotificationRepository,
    private val authorizationService: AuthorizationService,
    private val eventBusService: EventBusService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String
) {
    private val log = logger<AdminNotificationHandler>()

    suspend fun getAdminNotificationList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val adminId = context.pathParam("id")?.trim() orBadRequest "Admin ID path param is required."
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."

        val statusStr = context.param("status")?.trim() orBadRequest "Status query param is required."
        val page = context.param("page")?.trim()?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.trim()?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val status = if (statusStr == "") {
            null
        } else {
            try {
                NotificationStatus.valueOf(statusStr)
            } catch (_: Exception) {
                throw BadRequestException("Invalid status query param.")
            }
        }
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity, adminId = adminId)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create query for get notification list by admin ID and status
                val query = adminNotificationRepository.findByAdminIdAndStatus(
                    adminId = loggedInAdmin.id,
                    status = status,
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

                // get notification list
                val adminNotifications = pagedList.list

                // get total rows from the future
                val totalData = pagedList.totalCount

                val paginationInfo = paginate(
                    "$apiBaseUrl/admins/$adminId/notifications?q=$keyword&status=$statusStr&",
                    page,
                    limit,
                    totalData.toLong()
                )

                // transform AdminNotification list to DTO class and return them with Pagination info as Pair
                adminNotifications.map { it.toAdminNotificationList() } to paginationInfo
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Get admin notification list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Get admin notification list from DB has been succeed"),
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
                "message" to "Admin notification list was successfully fetched."
            )
        }
    }

    suspend fun readAllAdminNotifications(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val adminId = context.pathParam("id")?.trim() orBadRequest "Admin ID path param is required."
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity, adminId = adminId)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get unread notification list by admin ID
                val unreadNotifications = adminNotificationRepository.findByAdminIdAndStatus(
                    adminId = loggedInAdmin.id,
                    status = NotificationStatus.NEW
                ).findList()

                // update notification status to READ if there is any unread notifications
                if (unreadNotifications.isNotEmpty()) {
                    val updatedNotifications = unreadNotifications.map { n ->
                        n.status = NotificationStatus.READ
                        n
                    }
                    adminNotificationRepository.updateAll(list = updatedNotifications)
                }
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Read all admin notifications from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Read all admin notifications from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        /**
         * Send event bus message to client
         *
         */
        val (_, notifTime) = executeTimeMillis {
            try {
                coroutineScope {
                    launch {
                        val unreadNotif = UnreadNotification(
                            id = adminId,
                            unreadCount = 0
                        )
                        val notifMessages = listOf(
                            "admins.unreadNotifsCount.$adminId" to unreadNotif.toJson()
                        )
                        eventBusService.publishMessages(notifMessages, emptyMap(), true)
                    }
                }
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Send event bus message to client has been failed."),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Send event bus message to client has been succeed."),
            "notifTime" to notifTime
        )
        /**
         * End of send event bus message to client
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "All admin notifications was successfully read."
            )
        }
    }

    suspend fun deleteAdminNotification(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val adminId = context.pathParam("adminId")?.trim() orBadRequest "Admin ID path param is required."
        val notificationId = context.pathParam("notificationId")?.trim() orBadRequest "Notification ID path param is required."

        val adminNotification = adminNotificationRepository.findByExternalId(notificationId)
            .findOne() orNotFound "Admin notification data not found."

        if (adminNotification.admin.externalId != adminId) {
            throw InvalidCredentials("You are not authorized to access this feature.")
        }
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        authorizationService.authorizeAdmin(identity = userIdentity, adminId = adminId)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // delete admin notification
                adminNotificationRepository.delete(o = adminNotification)
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Delete admin notification by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Delete admin notification by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        /**
         * Send event bus message to client
         *
         */
        val (_, notifTime) = executeTimeMillis {
            try {
                coroutineScope {
                    launch {
                        val notifMessages = listOf(
                            "admins.notifications.$adminId" to adminNotification.toAdminNotificationList().toJson()
                                .put("action", "delete")
                        )
                        eventBusService.publishMessages(notifMessages, emptyMap(), true)
                    }
                }
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Send event bus message to client has been failed."),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Send event bus message to client has been succeed."),
            "notifTime" to notifTime
        )
        /**
         * End of send event bus message to client
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Admin notification was successfully deleted."
            )
        }
    }

    suspend fun clearAdminNotifications(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val adminId = context.pathParam("id")?.trim() orBadRequest "Admin ID path param is required."
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity, adminId = adminId)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get notification list by admin ID
                val adminNotifications = adminNotificationRepository.findByAdminIdAndStatus(
                    adminId = loggedInAdmin.id,
                    status = null
                ).findList()

                // delete all admin notifications if there is any notifications
                if (adminNotifications.isNotEmpty()) {
                    adminNotificationRepository.deleteAll(list = adminNotifications)
                }
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Delete all admin notifications by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Delete all admin notifications by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        /**
         * Send event bus message to client
         *
         */
        val (_, notifTime) = executeTimeMillis {
            try {
                coroutineScope {
                    launch {
                        val messageObj = json {
                            obj(
                                "id" to adminId,
                                "action" to "clear"
                            )
                        }
                        val unreadNotif = UnreadNotification(
                            id = adminId,
                            unreadCount = 0
                        )

                        val notifMessages = listOf(
                            "admins.notifications.$adminId" to messageObj,
                            "admins.unreadNotifsCount.$adminId" to unreadNotif.toJson()
                        )
                        eventBusService.publishMessages(notifMessages, emptyMap(), true)
                    }
                }
            } catch (ex: Exception) {
                log.notifLog(
                    ERROR("Send event bus message to client has been failed."),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.notifLog(
            INFO("Send event bus message to client has been succeed."),
            "notifTime" to notifTime
        )
        /**
         * End of send event bus message to client
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "All admin notifications was successfully cleared."
            )
        }
    }
}
