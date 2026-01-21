package com.starter.app.domain.setting.db.repository

import com.starter.app.domain.setting.db.model.Banner
import com.starter.app.domain.setting.db.model.value.BannerType
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.CacheMode
import io.ebean.Database
import io.ebean.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class BannerRepository @Inject constructor(ebeanServer: Database):
    Repository<Banner, Long>(ebeanServer, Banner::class) {

    suspend fun findByTypeAndStatus(type: BannerType, status: Boolean): Query<Banner> {
        return query
            .select("*")
            .where()
            .and()
                .eq("type", type)
                .eq("status", status)
            .endAnd()
            .query()
            .setAutoTune(true)
            .setUseQueryCache(true)
            .setBeanCacheMode(CacheMode.AUTO)
    }

    suspend fun findBannerByStatusAndTitleContain(status: Boolean?, keyword: String): Query<Banner> {
        return if (status != null) {
            query
                .select("*")
                .where()
                .and()
                    .ne("type", BannerType.HOMEPAGE_SLIDESHOW)
                    .eq("status", status)
                    .icontains("title", keyword)
                .endAnd()
                .query()
                .setAutoTune(true)
                .setUseQueryCache(true)
                .setBeanCacheMode(CacheMode.AUTO)
        } else {
            query
                .select("*")
                .where()
                .and()
                    .ne("type", BannerType.HOMEPAGE_SLIDESHOW)
                    .icontains("title", keyword)
                .endAnd()
                .query()
                .setAutoTune(true)
                .setUseQueryCache(true)
                .setBeanCacheMode(CacheMode.AUTO)
        }
    }

    suspend fun findSlideshowByStatusAndTitleContain(status: Boolean?, keyword: String): Query<Banner> {
        return if (status != null) {
            query
                .select("*")
                .where()
                .and()
                    .eq("type", BannerType.HOMEPAGE_SLIDESHOW)
                    .eq("status", status)
                    .icontains("title", keyword)
                .endAnd()
                .query()
                .setAutoTune(true)
                .setUseQueryCache(true)
                .setBeanCacheMode(CacheMode.AUTO)
        } else {
            query
                .select("*")
                .where()
                .and()
                    .eq("type", BannerType.HOMEPAGE_SLIDESHOW)
                    .icontains("title", keyword)
                .endAnd()
                .query()
                .setAutoTune(true)
                .setUseQueryCache(true)
                .setBeanCacheMode(CacheMode.AUTO)
        }
    }

    suspend fun findBannerById(id: Long?): Query<Banner> {
        val bannerID = id orDataError "Invalid banner ID."
        return query
            .select("*")
            .where()
            .and()
                .idEq(bannerID)
                .ne("type", BannerType.HOMEPAGE_SLIDESHOW)
            .endAnd()
            .query()
            .setAutoTune(true)
            .setUseQueryCache(true)
            .setBeanCacheMode(CacheMode.AUTO)
    }

    suspend fun findSlideshowById(id: Long?): Query<Banner> {
        val slideshowID = id orDataError "Invalid slideshow ID."
        return query
            .select("*")
            .where()
            .and()
                .idEq(slideshowID)
                .eq("type", BannerType.HOMEPAGE_SLIDESHOW)
            .endAnd()
            .query()
            .setAutoTune(true)
            .setUseQueryCache(true)
            .setBeanCacheMode(CacheMode.AUTO)
    }
}
