package com.starter.app.domain.notification.db.repository

import com.starter.app.domain.notification.db.model.AdminNotification
import com.starter.app.domain.notification.db.model.value.NotificationStatus
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for [AdminNotification] bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AdminNotificationRepository @Inject constructor(ebeanServer: Database):
    Repository<AdminNotification, Long>(ebeanServer, AdminNotification::class) {

    suspend fun findByExternalId(externalId: String): Query<AdminNotification> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByAdminIdAndStatus(
        adminId: Long?,
        status: NotificationStatus? = null,
        keyword: String = ""
    ): Query<AdminNotification> {
        val adminID = adminId orDataError "Invalid admin ID."
        val q = query
            .select("id, externalId, message, status, createdAt")
            .fetchLazy("admin", "id")
            .fetchLazy("actor", "*")
            .where()
            .and()
                .eq("admin.id", adminID)

        return q.run {
            if (status == null) {
                this
            } else {
                this
                    .eq("status", status)
            }
        }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .icontains("message", keyword)
                }
            }
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByAdminIdsAndStatus(
        adminIds: List<Long?>,
        status: NotificationStatus? = null
    ): Query<AdminNotification> {
        val q = query
            .select("id")
            .fetchLazy("admin", "id, externalId")
            .fetchLazy("actor", "id")
            .where()
            .and()
                .isIn("admin.id", adminIds)

        return q.run {
            if (status == null) {
                this
            } else {
                this
                    .eq("status", status)
            }
        }
            .endAnd()
            .query()
            .setAutoTune(false)
    }
}
