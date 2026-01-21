package com.starter.app.domain.user.db.repository

import com.starter.app.domain.user.db.model.CmsUser
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import com.starter.library.extension.validate
import com.starter.library.extension.validateUnique
import id.yoframework.ebean.repository.Repository
import id.yoframework.web.exception.orDataError
import io.ebean.CacheMode
import io.ebean.Database
import io.ebean.Query
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data Access Object (DAO) for CmsUser bean entity
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class CmsUserRepository @Inject constructor(ebeanServer: Database):
    Repository<CmsUser, Long>(ebeanServer, CmsUser::class) {

    suspend fun findByExternalId(externalId: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .eq("externalId", externalId)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findDueLockedUserAccounts(): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
                .eq("locked", true)
                .le("autoUnlockedAt", LocalDateTime.now())
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByCreatedByOrLastUpdatedBy(userId: Long?): Query<CmsUser> {
        val userID = userId orDataError "Invalid user ID."
        return query
            .select("*")
            .where()
            .or()
                .eq("createdBy.id", userID)
                .eq("lastUpdatedBy.id", userID)
            .endOr()
            .query()
            .setAutoTune(false)
    }

    suspend fun findByRoleId(roleId: Long?, status: CmsUserStatus? = null): Query<CmsUser> {
        val roleID = roleId orDataError "Invalid role ID."
        val q = query
            .select("id")
            .where()
            .and()
                .eq("role.id", roleID)

        return q.run {
            if (status == null) {
                this
            } else {
                this
                    .eq("status", status)
            }
        }
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByEmailOrNewEmail(email: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .or()
                .ieq("email", email)
                .ieq("newEmail", email)
            .endOr()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByEmail(email: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .ieq("email", email)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByUsername(username: String): Query<CmsUser> {
        return query
            .select("username")
            .where()
            .ieq("username", username)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByPhone(phone: String): Query<CmsUser> {
        return query
            .select("phone")
            .where()
            .and()
            .eq("phone", phone)
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByIdentity(identity: String): Query<CmsUser> {
        return query
            .select("*")
            .fetch("role", "*")
            .fetch("createdBy", "*")
            .fetch("lastUpdatedBy", "*")
            .fetch("setting", "*")
            .fetch("admin", "*")
            .fetch("apiKey", "*")
            .where()
            .or()
                .ieq("email", identity)
                .ieq("username", identity)
                .eq("phone", identity)
            .endOr()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByIdentities(identities: List<String?>): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .or()
                .isIn("email", identities)
                .isIn("username", identities)
                .isIn("phone", identities)
            .endOr()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByEmailOrUsername(identity: String, excludedUserId: String): Query<CmsUser> {
        val q = query
            .select("*")
            .where()
            .and()

        return q.run {
            if (excludedUserId == "") {
                this
            } else {
                this
                    .ne("id", excludedUserId.toLong())
            }
        }
            .run {
                this
                    .or()
                        .ieq("email", identity)
                        .ieq("username", identity)
                    .endOr()
                    .endAnd()
                    .query()
                    .setAutoTune(false)
                    .setUseQueryCache(false)
                    .setBeanCacheMode(CacheMode.OFF)
            }
    }

    suspend fun findByEmailAndForgotCode(email: String, forgotCode: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
            .ieq("email", email)
            .eq("forgotCode", forgotCode)
            .gt("forgotCodeExpiredOn", LocalDateTime.now())
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByIdentityAnd2FACode(identity: String, login2FACode: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
                .or()
                    .ieq("email", identity)
                    .ieq("username", identity)
                    .eq("phone", identity)
                .endOr()
                .eq("login2FACode", login2FACode)
                .gt("login2FACodeExpiredOn", LocalDateTime.now())
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findActiveTokenByEmail(email: String, token: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
                .eq("token", token)
                .gt("tokenExpiredOn", LocalDateTime.now())
                .ieq("newEmail", email)
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findActiveTokenByPhone(phone: String, tokenPhone: String): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
                .eq("phone", phone)
                .eq("tokenPhone", tokenPhone)
                .eq("status", CmsUserStatus.ACTIVE)
                .eq("phoneVerified", false)
                .gt("tokenPhoneExpiredOn", LocalDateTime.now())
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByEmailAndVerifiedStatus(email: String, emailVerified: Boolean): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
                .eq("emailVerified", emailVerified)
                .ieq("newEmail", email)
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun findByPhoneAndVerifiedStatus(phone: String, phoneVerified: Boolean): Query<CmsUser> {
        return query
            .select("*")
            .where()
            .and()
                .eq("phone", phone)
                .eq("phoneVerified", phoneVerified)
            .endAnd()
            .query()
            .setAutoTune(false)
            .setUseQueryCache(false)
            .setBeanCacheMode(CacheMode.OFF)
    }

    suspend fun updateEmailAndToken(cmsUser: CmsUser, email: String, newToken: String?): Boolean {
        val id = cmsUser.id orDataError "Invalid user data."

        cmsUser.newEmail = email
        cmsUser.token = newToken

        cmsUser.lastSendToken = LocalDateTime.now()
        cmsUser.tokenExpiredOn = LocalDateTime.now().plusHours(24)
        cmsUser.emailVerified = false

        cmsUser.validate()
        cmsUser.validateUnique(ebeanDatabase = ebean)

        return update(code = id, o = cmsUser)
    }

    suspend fun updateEmailVerifiedStatus(cmsUser: CmsUser, email: String, verifiedStatus: Boolean): Boolean {
        val id = cmsUser.id orDataError "Invalid user data."

        cmsUser.email = email
        cmsUser.newEmail = null
        cmsUser.token = null

        cmsUser.tokenExpiredOn = null
        cmsUser.lastSendToken = null

        cmsUser.emailVerified = verifiedStatus
        cmsUser.status = CmsUserStatus.ACTIVE
        cmsUser.lastUpdatedBy = cmsUser

        return update(code = id, o = cmsUser)
    }

    suspend fun updatePhoneVerifiedStatus(cmsUser: CmsUser, verifiedStatus: Boolean): Boolean {
        val id = cmsUser.id orDataError "Invalid user data."

        cmsUser.phoneVerified = verifiedStatus
        cmsUser.tokenPhone = null

        cmsUser.tokenPhoneExpiredOn = null
        cmsUser.lastSendTokenPhone = null
        cmsUser.lastUpdatedBy = cmsUser

        return update(code = id, o = cmsUser)
    }
}
