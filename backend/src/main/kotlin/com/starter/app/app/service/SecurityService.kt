package com.starter.app.app.service

import com.starter.app.app.service.plain.AuthenticatedSocialUser
import com.starter.app.app.service.plain.AuthenticatedUser
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import com.starter.app.domain.auth.db.model.value.SocialPlatform
import com.starter.app.domain.auth.db.repository.AccessRoleRepository
import com.starter.app.domain.auth.db.repository.ApiKeyRepository
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import com.starter.app.domain.user.db.repository.CmsUserRepository
import id.yoframework.core.json.get
import id.yoframework.extra.extension.password.PlainPassword
import id.yoframework.extra.extension.password.match
import id.yoframework.web.exception.*
import io.ebean.Transaction
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.authentication.TokenCredentials
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.coAwait
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class SecurityService @Inject constructor(
    private val cmsUserRepository: CmsUserRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val apiKeyRepository: ApiKeyRepository,
    @param:Named("jwtAuthProvider") private val jwtAuthProvider: JWTAuth,
    @param:Named("maxLoginAttempt") private val maxLoginAttempt: Int,
    @param:Named("autoUnlockedAccountAfter") private val autoUnlockedAccountAfter: Long
) {

    private suspend fun authenticate(authInfo: JsonObject, ebeanTransaction: Transaction): AuthenticatedUser {

        val identity = authInfo.get<String>("identity") orBadRequest "Invalid auth info"
        val password = authInfo.get<String>("password") orBadRequest "Invalid auth info"

        val user = cmsUserRepository.findByIdentity(identity)
            .findOne() orBadRequest "Could not find your account. Try another."
        val hashedPassword = user.password

        /**
         * Check whether the account is locked or not
         *
         */
        if (user.locked) {
            val timezoneOffset = user.setting.timezoneOffset
            val autoUnlockedAt = user.autoUnlockedAt
                ?.plusMinutes(timezoneOffset)
                ?.format(DateTimeFormatter.ofPattern("dd MMM YYYY HH:mm"))

            throw BadRequestException("Your account is locked because you have reached " +
                    "the maximum login attempts ($maxLoginAttempt times). Try again at $autoUnlockedAt.")
        }

        if (!password.match(hashedPassword)) {
            /**
             * Max Login Attempt Implementation
             *
             */
            val loginAttempt = user.loginAttempt
            user.loginAttempt = loginAttempt.inc()

            val errorMessage = if (user.loginAttempt == maxLoginAttempt) {
                user.locked = true
                user.autoUnlockedAt = LocalDateTime.now().plusHours(autoUnlockedAccountAfter)

                "Wrong password and you have reached the maximum login attempts ($maxLoginAttempt times). " +
                        "Try again in $autoUnlockedAccountAfter hour."
            } else {
                "Wrong password. Try again or click forgot password to reset it."
            }

            // update user locked data to DB
            val userId = user.id orDataError "Invalid user data."
            cmsUserRepository.update(code = userId, o = user)

            // commit DB Transaction
            ebeanTransaction.commit()

            throw BadRequestException(errorMessage)
        }

        if (user.status == CmsUserStatus.PENDING) {
            throw InvalidCredentials("Your email has not verified yet. Please verify your email first.")
        }
        if (user.status == CmsUserStatus.SUSPENDED) {
            throw InvalidCredentials("Your account has been suspended. Please contact administrator.")
        }

        val accessRoles = accessRoleRepository.findAllowedAccessByRole(user.role.id).findList()
        val allowedAccess = accessRoles.map { it.access.name }

        return AuthenticatedUser(
            user = user,
            accessList = allowedAccess
        )
    }

    private suspend fun socialAuthenticate(authInfo: JsonObject): AuthenticatedSocialUser {

        val email = authInfo.get<String>("email") orBadRequest "Invalid auth info."
        val user = cmsUserRepository.findByIdentity(email).findOne()

        return if (user != null) {

            if (user.status == CmsUserStatus.SUSPENDED) {
                throw InvalidCredentials("Your account has been suspended. Please contact administrator.")
            }

            val accessRoles = accessRoleRepository.findAllowedAccessByRole(user.role.id).findList()
            val allowedAccess = accessRoles.map { it.access.name }

            AuthenticatedSocialUser(
                user = user,
                accessList = allowedAccess
            )
        } else {
            AuthenticatedSocialUser(
                user = null,
                accessList = emptyList()
            )
        }
    }

    suspend fun authenticate(identity: String, plainPassword: PlainPassword, ebeanTransaction: Transaction): AuthenticatedUser {
        val authInfo = json {
            obj(
                "identity" to identity,
                "password" to plainPassword
            )
        }
        return authenticate(authInfo, ebeanTransaction)
    }

    suspend fun socialAuthenticate(email: String, platform: SocialPlatform): AuthenticatedSocialUser {
        val authInfo = json {
            obj(
                "email" to email,
                "platform" to platform.toString()
            )
        }
        return socialAuthenticate(authInfo)
    }

    private suspend fun authenticateToken(authInfo: JsonObject): User {

        val path = authInfo.get<String>("path") orBadRequest "Invalid auth info path."
        val accessToken = authInfo.get<String>("accessToken") orBadRequest "Invalid auth info access token."

        val apiKey = apiKeyRepository.findByAccessToken(accessToken).findOne() orUnauthorized "Invalid or expired access token. Please log in again to obtains a new token."
        val userStatus = apiKey.cmsUser?.status

        // temporarily white list /voucher-members resource
        if (userStatus == CmsUserStatus.PENDING && !path.contains("/auth") &&
            !path.contains("/accounts") && !path.contains("/members") &&
            !path.contains("/hotels") && !path.contains("/countries") &&
            !path.contains("/voucher-members")) {
            throw ValidationException(listOf("Your email has not been verified. Please verify first to continue."))
        }

        val credentials = TokenCredentials(accessToken)
        return try {
            jwtAuthProvider.authenticate(
                credentials,
            ).coAwait()
        } catch (ex: Exception) {
            throw ex
        }
    }

    suspend fun authenticateToken(accessToken: String, context: RoutingContext): User {
        val authInfo = json {
            obj(
                "accessToken" to accessToken,
                "path" to context.normalizedPath()
            )
        }
        return authenticateToken(authInfo)
    }

    suspend fun isUserAuthorized(userRole: String, accessName: String): Boolean {
        val allowedPermissionCount = accessRoleRepository.findByAccessNameAndRoleNameAndPermission(
            accessName = accessName,
            roleName = userRole,
            permission = AccessRolePermission.ALLOWED
        ).findCount()
        if (allowedPermissionCount == 0) throw InvalidCredentials("You are not authorized to access this feature.")
        return true
    }
}
