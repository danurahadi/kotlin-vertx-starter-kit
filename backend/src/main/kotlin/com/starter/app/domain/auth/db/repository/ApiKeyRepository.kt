package com.starter.app.domain.auth.db.repository

import com.starter.app.domain.auth.db.model.ApiKey
import com.starter.app.domain.user.db.model.CmsUser
import com.starter.library.extension.validate
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.Database
import io.ebean.Query
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for ApiKey bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class ApiKeyRepository @Inject constructor(ebeanServer: Database) :
    Repository<ApiKey, Long>(ebeanServer, ApiKey::class) {

    suspend fun findByAccessToken(accessToken: String): Query<ApiKey> {
        return query
            .select("*")
            .where()
            .and()
            .eq("accessToken", accessToken)
            .gt("expiredTime", LocalDateTime.now())
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByExternalId(externalId: String): Query<ApiKey> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByTitle(title: String): Query<ApiKey> {
        return query
            .select("*")
            .where()
            .and()
                .isNull("cmsUser")
                .ieq("title", title)
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByApiKey(apiKey: String): Query<ApiKey> {
        return query
            .select("*")
            .where()
            .and()
                .isNull("cmsUser")
                .eq("apiKey", apiKey)
                .gt("expiredTime", LocalDateTime.now())
            .endAnd()
            .query()
            .setAutoTune(false)
    }

    suspend fun saveJwtTokenForUser(
        cmsUser: CmsUser,
        jwtToken: String,
        registrationToken: String = "",
        ipAddress: String,
        browserInfo: String,
        jwtExpiresInSeconds: Int
    ) {
        val userApiKey = ApiKey(
            cmsUser = cmsUser,
            title = "Internal Client App",
            accessToken = jwtToken,
            regToken = registrationToken,
            ipAddress = ipAddress,
            browser = browserInfo,
            location = "",
            operatingSystem = "",
            expiredTime = LocalDateTime.now().plusSeconds(jwtExpiresInSeconds.toLong())
        )

        // validate API key object
        userApiKey.validate()

        // insert API key data to DB
        return insert(userApiKey)
    }

    suspend fun findByUserId(userId: Long?): Query<ApiKey> {
        val userID = userId orDataError "Invalid user ID."
        return query
            .select("*")
            .where()
            .eq("cmsUser.id", userID)
            .query()
            .setAutoTune(false)
    }

    suspend fun findByTitleContaining(keyword: String): Query<ApiKey> {
        val q = query
            .select("*")
            .where()
            .and()
                .isNull("cmsUser")

        return q.run {
            if (keyword == "") {
                this
            } else {
                this
                    .icontains("title", keyword)
            }
        }
            .query()
            .setAutoTune(false)
    }
}
