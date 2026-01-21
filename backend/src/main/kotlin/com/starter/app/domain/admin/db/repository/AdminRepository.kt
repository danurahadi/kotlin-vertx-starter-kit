package com.starter.app.domain.admin.db.repository

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import id.yoframework.ebean.repository.Repository
import io.ebean.CacheMode
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for Admin bean entity
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AdminRepository @Inject constructor(ebeanServer: Database):
    Repository<Admin, Long>(ebeanServer, Admin::class) {

    suspend fun findByExternalId(externalId: String): Query<Admin> {
        return query
            .select("*")
            .fetch("cmsUser", "*")
            .fetch("cmsUser.setting", "*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByStatusAndNameContain(
        status: CmsUserStatus? = null,
        roleIds: List<Long?> = emptyList(),
        keyword: String = ""
    ): Query<Admin> {
        val q = query
            .select("*")
            .fetch("cmsUser", "*")
            .where()
            .and()

        return q.run {
            if (status == null) {
                this
            } else {
                this
                    .eq("cmsUser.status", status)
            }
        }
            .run {
                if (roleIds.isNotEmpty()) {
                    this
                        .isIn("cmsUser.role.id", roleIds)
                } else {
                    this
                }
            }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .or()
                            .icontains("fullName", keyword)
                            .icontains("cmsUser.email", keyword)
                        .endOr()
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
