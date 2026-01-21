package com.starter.app.domain.setting.db.repository

import com.starter.app.domain.setting.db.model.Country
import id.yoframework.ebean.repository.Repository
import io.ebean.CacheMode
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for [Country] bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class CountryRepository @Inject constructor(ebeanServer: Database):
    Repository<Country, Long>(ebeanServer, Country::class) {

    suspend fun findByExternalId(externalId: String): Query<Country> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByAlpha2Code(alpha2Code: String): Query<Country> {
        return query
            .select("*")
            .where()
            .eq("alpha2Code", alpha2Code)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(true)
            .setBeanCacheMode(CacheMode.AUTO)
    }

    suspend fun findByAlpha3Code(alpha3Code: String): Query<Country> {
        return query
            .select("*")
            .where()
            .eq("alpha3Code", alpha3Code)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(true)
            .setBeanCacheMode(CacheMode.AUTO)
    }

    suspend fun findByCodeOrNameContain(keyword: String): Query<Country> {
        val q = query
            .select("*")
            .where()

        return q.run {
            if (keyword == "") {
                this
            } else {
                this
                    .or()
                        .icontains("alpha2Code", keyword)
                        .icontains("name", keyword)
                    .endOr()
            }
        }
            .run {
                this
                    .query()
                    .setAutoTune(false)
                    .setUseQueryCache(true)
                    .setBeanCacheMode(CacheMode.AUTO)
            }
    }
}
