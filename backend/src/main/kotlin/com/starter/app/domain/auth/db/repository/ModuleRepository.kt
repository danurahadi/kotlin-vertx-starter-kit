package com.starter.app.domain.auth.db.repository

import com.starter.app.domain.auth.db.model.Module
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for Module bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ModuleRepository @Inject constructor(ebeanServer: Database):
    Repository<Module, Long>(ebeanServer, Module::class) {

    suspend fun findByExternalId(externalId: String): Query<Module> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByCreatedByOrLastUpdatedBy(adminId: Long?): Query<Module> {
        val adminID = adminId orDataError "Invalid admin ID."
        return query
            .select("*")
            .where()
            .or()
                .eq("createdBy.id", adminID)
                .eq("lastUpdatedBy.id", adminID)
            .endOr()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByCodeOrNameContain(keyword: String): Query<Module> {
        val q = query
            .select("*")
            .where()

        return q.run {
            if (keyword == "") {
                this
            } else {
                this
                    .or()
                        .icontains("code", keyword)
                        .icontains("name", keyword)
                    .endOr()
            }
        }
            .run {
                this
                    .query()
                    .setAutoTune(false)
            }
    }
}
