package com.starter.app.domain.user.db.repository

import com.starter.app.domain.user.db.model.CmsUserSetting
import id.yoframework.ebean.repository.Repository
import io.ebean.CacheMode
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for CmsUserSetting bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class CmsUserSettingRepository @Inject constructor(ebeanServer: Database):
    Repository<CmsUserSetting, Long>(ebeanServer, CmsUserSetting::class) {

    suspend fun findByExternalId(externalId: String): Query<CmsUserSetting> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }
}
