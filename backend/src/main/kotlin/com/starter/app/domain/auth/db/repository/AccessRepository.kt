package com.starter.app.domain.auth.db.repository

import com.starter.app.domain.auth.db.model.Access
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for Access bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AccessRepository @Inject constructor(ebeanServer: Database):
    Repository<Access, Long>(ebeanServer, Access::class) {

    suspend fun findAllAccess(): Query<Access> {
        return query
            .select("*")
            .where()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByExternalId(externalId: String): Query<Access> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByNames(names: List<String>): Query<Access> {
        return query
            .select("*")
            .where()
            .isIn("name", names)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByModuleId(moduleId: Long?): Query<Access> {
        val moduleID = moduleId orDataError "Invalid module ID."
        return query
            .select("*")
            .where()
            .eq("module.id", moduleID)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByNameOrAliasContain(moduleId: Long? = null, keyword: String = ""): Query<Access> {
        val q = query
            .select("*")
            .where()
            .and()

        return q.run {
            if (moduleId == null) {
                this
            } else {
                this
                    .eq("module.id", moduleId)
            }
        }
            .run {
                if (keyword == "") {
                    this
                } else {
                    this
                        .or()
                            .icontains("name", keyword)
                            .icontains("alias", keyword)
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
