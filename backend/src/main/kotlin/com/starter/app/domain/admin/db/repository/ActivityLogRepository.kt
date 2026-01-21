package com.starter.app.domain.admin.db.repository

import com.starter.app.domain.admin.db.model.ActivityLog
import id.yoframework.ebean.repository.Repository
import io.ebean.CacheMode
import io.ebean.Database
import io.ebean.Query
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for [ActivityLog] bean entity
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ActivityLogRepository @Inject constructor(ebeanServer: Database):
    Repository<ActivityLog, Long>(ebeanServer, ActivityLog::class) {

    suspend fun findByExternalId(externalId: String): Query<ActivityLog> {
        return query
            .select("*")
            .fetch("actor", "*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByDateTimeAndDescriptionContain(
        startDateTime: LocalDateTime? = null,
        endDateTime: LocalDateTime? = null,
        keyword: String = ""
    ): Query<ActivityLog> {
        val q = query
            .select("id, externalId, description, createdAt, ")
            .fetchLazy("actor", "id, externalId, fullName, thumbnailProfileImage")
            .fetchLazy("actor.cmsUser", "id, username")
            .where()
            .and()

        return q.run {
            if (startDateTime == null || endDateTime == null) {
                this
            } else {
                this
                    .ge("createdAt", startDateTime)
                    .le("createdAt", endDateTime)
            }
        }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .icontains("description", keyword)
                }
            }
            .run {
                this
                    .endAnd()
                    .query()
                    .setAutoTune(false)
                    .setUseQueryCache(false)
                    .setBeanCacheMode(CacheMode.OFF)
            }
    }
}
