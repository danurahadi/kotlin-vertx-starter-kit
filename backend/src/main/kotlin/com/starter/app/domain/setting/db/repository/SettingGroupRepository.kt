package com.starter.app.domain.setting.db.repository

import com.starter.app.domain.setting.db.model.SettingGroup
import id.yoframework.ebean.repository.Repository
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for [SettingGroup] bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class SettingGroupRepository @Inject constructor(ebeanServer: Database):
    Repository<SettingGroup, Long>(ebeanServer, SettingGroup::class) {

    suspend fun findByExternalId(externalId: String): Query<SettingGroup> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByNameContain(keyword: String): Query<SettingGroup> {
        val q = query
            .select("*")
            .where()
            .and()

        return q.run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .icontains("name", keyword)
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
