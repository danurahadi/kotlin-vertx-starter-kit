package com.starter.app.domain.auth.db.repository

import com.starter.app.domain.auth.db.model.Role
import id.yoframework.ebean.repository.Repository
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for Role bean entity
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class RoleRepository @Inject constructor(ebeanServer: Database):
    Repository<Role, Long>(ebeanServer, Role::class) {

    suspend fun findByExternalId(externalId: String): Query<Role> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByName(name: String): Query<Role> {
        return query
            .select("*")
            .where()
            .ieq("name", name)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByNameOrAliasContain(keyword: String = ""): Query<Role> {
        val q = query
            .select("*")
            .where()
            .and()

        return q.run {
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
