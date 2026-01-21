package com.starter.app.domain.user.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.user.db.model.CmsUser
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import com.starter.app.domain.user.db.repository.CmsUserRepository
import com.starter.app.domain.user.db.repository.CmsUserSettingRepository
import com.starter.app.domain.user.userLog
import com.starter.app.integration.mailer.MailerService
import com.starter.app.integration.sms.SmsService
import com.starter.library.extension.validate
import com.starter.library.extension.validateUnique
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.extra.extension.password.encode
import id.yoframework.extra.extension.password.match
import id.yoframework.extra.extension.pebble.Pebble
import id.yoframework.extra.extension.pebble.compileTemplate
import id.yoframework.extra.extension.pebble.evaluate
import id.yoframework.extra.snowflake.nextAlpha
import id.yoframework.web.exception.*
import id.yoframework.web.extension.jsonBody
import id.yoframework.web.extension.param
import io.ebean.Database
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.*
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.random.RandomGenerator
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [CmsUser] data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class CmsUserHandler @Inject constructor(
    private val cmsUserRepository: CmsUserRepository,
    private val cmsUserSettingRepository: CmsUserSettingRepository,
    private val mailerService: MailerService,
    private val ebeanServer: Database,
    private val smsService: SmsService,
    private val authorizationService: AuthorizationService,
    @param:Named("appName") private val appName: String,
    @param:Named("companyEmail") private val companyEmail: String,
    @param:Named("companyPhone") private val companyPhone: String,
    @param:Named("companyFacebookLink") private val companyFacebookLink: String,
    @param:Named("companyInstagramLink") private val companyInstagramLink: String,
    @param:Named("feAdminBaseUrl") private val feAdminBaseUrl: String
) {
    private val log = logger<CmsUserHandler>()

    suspend fun searchUser(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val identity = context.param("identity")?.trim() orBadRequest "Identity query param is required."
        val excludedUserId = context.param("excludedUserId")
            ?.trim() orBadRequest "Excluded user ID query param is required."
        
        if (identity == "") throw ValidationException(listOf("Identity could not be blank."))
        /**
         * End of validation process
         *
         */

        /**
         * Core process for search user by email or username
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                val user = cmsUserRepository.findByEmailOrUsername(
                    identity = identity,
                    excludedUserId = excludedUserId
                ).findOne() orNotFound "User data not found. Try another email / username."

                user.toResponseCompact()
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Search user by email or username has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Search user by email or username has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Search user by identity has been succeed."
            )
        }
    }

    suspend fun changeEmail(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val userId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val oldEmail = body.get<String>("oldEmail")?.trim() orBadRequest "Old email body param is required."
        val newEmail = body.get<String>("newEmail")?.trim() orBadRequest "New email body param is required."

        if (oldEmail == "") throw ValidationException(listOf("Old email could not be blank."))
        if (newEmail == "") throw ValidationException(listOf("New email could not be blank."))

        if (newEmail == oldEmail) {
            throw ValidationException(listOf("Email must differ from old email."))
        }

        val existingEmail = cmsUserRepository.findByEmailOrNewEmail(newEmail).findCount()
        if (existingEmail > 0) {
            throw ValidationException(listOf("Email already used by another user. Try another email."))
        }
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity, userId = userId)

        if (oldEmail != loggedInUser.email) {
            throw InvalidCredentials("Your email was incorrect.")
        }
        /**
         * End of authentication process
         *
         */

        /**
         * Core process for update user's email
         *
         */
        val (_, emailTime) = executeTimeMillis {
            try {
                // update user's token email
                val newToken = nextAlpha(19)
                cmsUserRepository.updateEmailAndToken(
                    cmsUser = loggedInUser,
                    email = newEmail,
                    newToken = newToken
                )

                CoroutineScope(Dispatchers.IO).launch {
                    // send verification link to the user's new email
                    val subject = "Email verification"
                    val verificationLink = "$feAdminBaseUrl/verification?email=$newEmail&token=$newToken"

                    val emailTemplate = "templates/email-verification.peb"
                    val pebbleTemplate = Pebble.compileTemplate(templateLocation = emailTemplate)

                    val params = mutableMapOf(
                        "appName" to appName,
                        "companyEmail" to companyEmail,
                        "companyPhone" to companyPhone,
                        "companyFacebookLink" to companyFacebookLink,
                        "companyInstagramLink" to companyInstagramLink,
                        "fullName" to loggedInUser.admin.fullName,
                        "verificationLink" to verificationLink
                    )

                    val htmlTemplate = pebbleTemplate.evaluate(
                        parameters = params,
                        locale = Locale.getDefault()
                    )
                    mailerService.sendMessage(
                        to = newEmail,
                        subject = subject,
                        html = htmlTemplate
                    )
                }
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Email could not be updated to DB and/or verification email could not be sent"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Email has been updated to DB and verification email has been sent"),
            "emailTime" to emailTime
        )
        /**
         * End of core process for update user's email
         *
         */

        return json {
            obj(
                "data" to loggedInUser.toResponseDetail(),
                "message" to "Email was successfully updated but it won't be applied " +
                        "unless you verify it. Please verify it soon."
            )
        }
    }

    suspend fun changeUsername(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val oldUsername = body.get<String>("oldUsername")?.trim() orBadRequest "Old username body param is required."
        val newUsername = body.get<String>("newUsername")?.trim() orBadRequest "New username body param is required."

        if (oldUsername == "") throw ValidationException(listOf("Old username could not be blank."))
        if (newUsername == oldUsername) {
            throw ValidationException(listOf("Username must differ from old username."))
        }
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity, userId = externalId)

        if (oldUsername != loggedInUser.username) {
            throw InvalidCredentials("Your username was incorrect.")
        }
        /**
         * End of authentication process
         *
         */

        /**
         * Core process for update user's username
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                val id = loggedInUser.id orDataError "Invalid user data."
                loggedInUser.username = newUsername

                // validate user object
                loggedInUser.validate()
                loggedInUser.validateUnique(ebeanDatabase = ebeanServer)

                cmsUserRepository.update(code = id, o = loggedInUser)
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Username could not be updated to DB"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Username has been updated to DB"),
            "coreTime" to coreTime
        )
        /**
         * End of core process for update user's username
         *
         */

        return json {
            obj(
                "data" to loggedInUser.toResponseDetail(),
                "message" to "Username was successfully updated."
            )
        }
    }

    suspend fun changePhoneNumber(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val userId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val oldPhone = body.get<String>("oldPhone")?.trim()
        val newPhone = body.get<String>("newPhone")?.trim() orBadRequest "New phone body param is required."

        if (oldPhone == "") throw ValidationException(listOf("Old phone number could not be blank."))
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity, userId = userId)

        if (oldPhone != loggedInUser.phone) {
            throw InvalidCredentials("Your phone number was incorrect.")
        }
        if (newPhone == oldPhone) {
            throw ValidationException(listOf("Phone number must differ from old phone number."))
        }
        /**
         * End of authentication process
         *
         */

        /**
         * Core process for update user's phone number
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // update user's token phone and phone number
                val newTokenPhone = RandomStringUtils.random(
                    6, 0, 0,
                    false, true, null,
                    Random.from(RandomGenerator.getDefault())
                )
                val userID = loggedInUser.id orDataError "Invalid user data."

                // set new value to the user's field
                val tokenExpiredAt = LocalDateTime.now().plusMinutes(15)

                loggedInUser.phone = newPhone
                loggedInUser.tokenPhone = newTokenPhone
                loggedInUser.phoneVerified = false

                loggedInUser.lastSendTokenPhone = LocalDateTime.now()
                loggedInUser.tokenPhoneExpiredOn = tokenExpiredAt

                // validate user object
                loggedInUser.validate()
                loggedInUser.validateUnique(ebeanServer)

                cmsUserRepository.update(code = userID, o = loggedInUser)

                CoroutineScope(Dispatchers.IO).launch {
                    // send new verification code to the user's phone number via SMS
                    val expiredAt = tokenExpiredAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                    val textMessage = "$appName - Kode verifikasi untuk akunmu : $newTokenPhone, berlaku hingga " +
                            "$expiredAt. JANGAN BERIKAN KODE INI KE SIAPA PUN."

                    smsService.sendSMS(
                        recipient = newPhone,
                        textMessage = textMessage
                    )
                }
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Phone number could not be updated to DB and/or " +
                            "SMS verification code could not be sent"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Phone number has been updated to DB and SMS verification code has been sent."),
            "coreTime" to coreTime
        )
        /**
         * End of core process for update user's phone number
         *
         */

        return json {
            obj(
                "data" to loggedInUser.toResponseDetail(),
                "message" to "Phone number was successfully updated."
            )
        }
    }

    suspend fun changePassword(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity, userId = externalId)
        /**
         * End of authentication process
         *
         */

        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val oldPassword = body.get<String>("oldPassword")?.trim()
        val newPassword = body.get<String>("newPassword")?.trim() orBadRequest "New password body param is required."

        val newPasswordConfirm = body.get<String>("newPasswordConfirm")
            ?.trim() orBadRequest "New password confirm body param is required."

        val savedPassword = loggedInUser.password
        if (oldPassword != null) {
            if (!oldPassword.match(savedPassword)) {
                throw ValidationException(listOf("Your password was incorrect."))
            }
            if (newPassword == oldPassword) {
                throw ValidationException(listOf("Password must differ from old password."))
            }
        }

        if (newPassword != newPasswordConfirm) {
            throw ValidationException(listOf("Passwords do not match."))
        }

        // validate user object
        loggedInUser.password = newPassword
        loggedInUser.validate()
        /**
         * End of validation process
         *
         */

        /**
         * Core process for update user's password
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                val userId = loggedInUser.id orDataError "Invalid user data."
                loggedInUser.password = newPassword.encode()
                cmsUserRepository.update(code = userId, o = loggedInUser)
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Password could not be updated to DB"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Password has been updated to DB"),
            "coreTime" to coreTime
        )
        /**
         * End of core process for update user's password
         *
         */

        return json {
            obj(
                "data" to loggedInUser.toResponseDetail(),
                "message" to "Password was successfully updated."
            )
        }
    }

    suspend fun unlockAccount(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val user = cmsUserRepository.findByExternalId(externalId).findOne() orNotFound "User data not found."

        if (!user.locked) {
            throw ValidationException(listOf("User is already unlocked."))
        }
        if (user.admin.superadmin) {
            throw ValidationException(listOf("Unlock superadmin account is not allowed."))
        }
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // set user locked to false
                val id = user.id orDataError "Invalid user data."

                user.locked = false
                user.autoUnlockedAt = null
                user.loginAttempt = 0

                cmsUserRepository.update(code = id, o = user)
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Update user locked to false has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Update user locked to false has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to user.toResponseDetail(),
                "message" to "User account was successfully unlocked."
            )
        }
    }

    suspend fun suspendAccount(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val user = cmsUserRepository.findByExternalId(externalId).findOne() orNotFound "User data not found."

        if (user.status == CmsUserStatus.SUSPENDED) {
            throw ValidationException(listOf("User is already suspended."))
        }
        if (user.admin.superadmin) {
            throw ValidationException(listOf("Suspend superadmin account is not allowed."))
        }
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // set user status to SUSPENDED
                val id = user.id orDataError "Invalid user data."
                user.status = CmsUserStatus.SUSPENDED

                // delete related api key to user
                user.apiKey = null

                cmsUserRepository.update(code = id, o = user)
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Update user status to SUSPENDED has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Update user status to SUSPENDED has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to user.toResponseDetail(),
                "message" to "User account was successfully suspended."
            )
        }
    }

    suspend fun reactivateAccount(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val user = cmsUserRepository.findByExternalId(externalId).findOne() orNotFound "User data not found."

        if (user.status == CmsUserStatus.ACTIVE) {
            throw ValidationException(listOf("User is already active."))
        }
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                val id = user.id orDataError "Invalid user data."

                // set user status to ACTIVE
                user.status = CmsUserStatus.ACTIVE

                cmsUserRepository.update(code = id, o = user)
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Update user status to ACTIVE has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Update user status to ACTIVE has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to user.toResponseDetail(),
                "message" to "User account was successfully reactivated."
            )
        }
    }

    suspend fun updateSettings(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid user ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val timezoneOffset = body.get<Long>("timezoneOffset") orBadRequest "Timezone offset body param is required."
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity, userId = externalId)

        if (externalId != loggedInUser.externalId) {
            throw InvalidCredentials("You are not authorized to access this feature.")
        }
        /**
         * End of authentication process
         *
         */

        /**
         * Core process for update user's settings
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                val userSetting = loggedInUser.setting
                val userSettingID = userSetting.id orDataError "Invalid user setting data."

                userSetting.timezoneOffset = timezoneOffset

                // validate user setting object
                userSetting.validate()

                // update user setting data to DB
                cmsUserSettingRepository.update(code = userSettingID, o = userSetting)

                val updatedUser = cmsUserRepository.findByExternalId(loggedInUser.externalId)
                    .findOne() orNotFound "User data not found."

                updatedUser
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("User settings could not be updated to DB"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("User settings has been updated to DB"),
            "coreTime" to coreTime
        )
        /**
         * End of core process for update user's settings
         *
         */

        return json {
            obj(
                "data" to data.toResponseDetail(),
                "message" to "User settings was successfully updated."
            )
        }
    }

    suspend fun autoUnlockUsers(): Boolean {
        /**
         * Auto unlock 'Due' Locked User Accounts
         *
         */
        val (_, unlockUserTime) = executeTimeMillis {
            try {
                val lockedUsers = cmsUserRepository.findDueLockedUserAccounts().findList()
                if (lockedUsers.isNotEmpty()) {
                    val updatedUsers = lockedUsers.map { u ->
                        u.locked = false
                        u.autoUnlockedAt = null
                        u.loginAttempt = 0

                        u
                    }
                    // update users data to DB
                    cmsUserRepository.updateAll(updatedUsers)
                }
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Update Due Locked User Accounts to DB has been failed."),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Update Due Locked User Accounts to DB has been succeed."),
            "unlockUserTime" to unlockUserTime
        )
        /**
         * End of auto unlock 'Due' Locked User Accounts
         *
         */

        return true
    }
}
