package com.starter.app.domain.setting.db.repository

import com.starter.app.domain.setting.db.model.AppSetting
import id.yoframework.ebean.repository.Repository
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for [AppSetting] bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AppSettingRepository @Inject constructor(ebeanServer: Database):
    Repository<AppSetting, Long>(ebeanServer, AppSetting::class) {

    suspend fun findByExternalId(externalId: String): Query<AppSetting> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByExternalIdOrKey(id: String): Query<AppSetting> {
        return query
            .select("*")
            .where()
            .or()
                .eq("externalId", id)
                .ieq("settingKey", id)
            .endOr()
            .query()
            .setAutoTune(false)
    }

    suspend fun findBySettingGroupIdAndKeyContain(settingGroupId: Long?, keyword: String): Query<AppSetting> {
        val q = query
            .select("*")
            .where()
            .and()

        return q.run {
            if (settingGroupId == null) {
                this
            } else {
                this
                    .eq("settingGroup.id", settingGroupId)
            }
        }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .or()
                            .icontains("settingKey", keyword)
                            .icontains("settingValue", keyword)
                        .endOr()
                }
            }
            .run {
                this
                    .endAnd()
                    .query()
                    .setAutoTune(false)
            }
    }
}
