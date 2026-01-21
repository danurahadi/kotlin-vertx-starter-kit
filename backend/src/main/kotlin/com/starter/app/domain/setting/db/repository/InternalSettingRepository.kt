package com.starter.app.domain.setting.db.repository

import com.starter.app.domain.setting.db.model.InternalSetting
import id.yoframework.ebean.repository.Repository
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for [InternalSetting] bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class InternalSettingRepository @Inject constructor(ebeanServer: Database):
    Repository<InternalSetting, Long>(ebeanServer, InternalSetting::class) {

    suspend fun findBySettingKey(settingKey: String): Query<InternalSetting> {
        return query
            .select("*")
            .where()
            .eq("settingKey", settingKey)
            .query()
            .setAutoTune(false)
    }
}
