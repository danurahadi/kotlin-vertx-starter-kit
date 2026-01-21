package com.starter.app.domain.auth.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.app.service.SecurityService
import com.starter.app.domain.auth.authLog
import com.starter.app.domain.auth.db.repository.AccessRoleRepository
import com.starter.app.domain.auth.db.repository.ApiKeyRepository
import com.starter.app.domain.auth.plain.Login
import com.starter.app.domain.auth.plain.LoginResponse
import com.starter.app.domain.user.db.model.value.OnlineStatus
import com.starter.app.domain.user.db.repository.CmsUserRepository
import com.starter.app.domain.user.userLog
import com.starter.app.integration.google.GoogleService
import com.starter.app.integration.mailer.MailerService
import com.starter.app.integration.sms.SmsService
import com.starter.library.extension.matchWithEmail
import com.starter.library.extension.validate
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.extension.time.toMilliSeconds
import id.yoframework.core.json.get
import id.yoframework.extra.extension.password.encode
import id.yoframework.extra.extension.pebble.Pebble
import id.yoframework.extra.extension.pebble.compileTemplate
import id.yoframework.extra.extension.pebble.evaluate
import id.yoframework.extra.snowflake.nextAlpha
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.ValidationException
import id.yoframework.web.exception.orBadRequest
import id.yoframework.web.exception.orDataError
import id.yoframework.web.exception.orNotFound
import id.yoframework.web.extension.getBrowserInfo
import id.yoframework.web.extension.getRemoteIpAddress
import id.yoframework.web.extension.jsonBody
import io.ebean.Database
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.random.RandomGenerator
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for authentication process such as Login and Logout
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class UserAuthenticationHandler @Inject constructor(
    private val ebeanServer: Database,
    private val securityService: SecurityService,
    private val apiKeyRepository: ApiKeyRepository,
    private val cmsUserRepository: CmsUserRepository,
    private val accessRoleRepository: AccessRoleRepository,
    private val mailerService: MailerService,
    private val smsService: SmsService,
    private val authorizationService: AuthorizationService,
    private val googleService: GoogleService,
    @param:Named("appName") private val appName: String,
    @param:Named("usedEnv") private val usedEnv: String,
    @param:Named("companyEmail") private val companyEmail: String,
    @param:Named("companyPhone") private val companyPhone: String,
    @param:Named("companyFacebookLink") private val companyFacebookLink: String,
    @param:Named("companyInstagramLink") private val companyInstagramLink: String,
    @param:Named("feAdminBaseUrl") private val feAdminBaseUrl: String,
    @param:Named("jwtAuthProvider") private val jwtAuthProvider: JWTAuth,
    @param:Named("jwtExpiresInSeconds") private val jwtExpiresInSeconds: Int,
    @param:Named("jwtAlgorithm") private val jwtAlgorithm: String
) {
    private val log = logger<UserAuthenticationHandler>()

    suspend fun login(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val identity = body.get<String>("identity")?.trim() orBadRequest "Identity body param is required."

        val password = body.get<String>("password")?.trim() orBadRequest "Password body param is required."
        val staySignedIn = body.get<Boolean>("staySignedIn") orBadRequest "Stay signed in body param is required."
        val registrationToken = body.get<String>("registrationToken")?.trim() orBadRequest "Registration token body param is required."

        val recaptchaToken = body.get<String>("recaptchaToken")
            ?.trim() orBadRequest "Recaptcha token body param is required."

        // instantiate Login object
        val login = Login(
            identity = identity,
            password = password,
            staySignedIn = staySignedIn
        )

        // validate Login object
        login.validate()
        /**
         * End of validation process
         *
         */

        /**
         * Core process for login user
         *
         */
        val (loginResp, loginTime) = executeTimeMillis {

            // start DB transaction
            val ebeanTransaction = ebeanServer.beginTransaction()

            // commit & finally close DB transaction
            ebeanTransaction.use {
                try {
                    /**
                     * Validate Google Recaptcha Token
                     *
                     */
                    if (usedEnv != "LOCAL") {
                        val ipAddress = context.getRemoteIpAddress()
                        val recaptchaResponse = googleService.verifyRecaptchaToken(recaptchaToken, ipAddress)

                        val recaptchaStatus = recaptchaResponse.success
                        val recaptchaScore = recaptchaResponse.score

                        if (!recaptchaStatus || (recaptchaScore != null && recaptchaScore < 0.5)) {
                            throw ValidationException(
                                listOf(
                                    "Recaptcha verification has been failed. Please try again."
                                )
                            )
                        }
                    }
                    /**
                     * End of Validate Google Recaptcha Token
                     *
                     */

                    val ipAddress = context.getRemoteIpAddress()
                    val browserInfo = context.getBrowserInfo()

                    val authenticatedUser = securityService.authenticate(
                        identity = identity,
                        plainPassword = password,
                        ebeanTransaction = ebeanTransaction
                    )
                    val user = authenticatedUser.user
                    val accessList = authenticatedUser.accessList

                    val apiKey = apiKeyRepository.findByUserId(userId = user.id).findOne()
                    if (apiKey != null) {
                        apiKeyRepository.delete(o = apiKey)
                    }

                    val tokenLifetime = if (staySignedIn) {
                        2592000
                    } else {
                        jwtExpiresInSeconds
                    }
                    val userRoleName = user.role.name

                    val token = jwtAuthProvider.generateToken(json {
                        obj(
                            "identity" to identity,
                            "roleName" to userRoleName
                        )
                    }, JWTOptions().apply {
                        algorithm = jwtAlgorithm
                        expiresInSeconds = tokenLifetime
                    })

                    apiKeyRepository.saveJwtTokenForUser(
                        cmsUser = user,
                        jwtToken = token,
                        registrationToken = registrationToken,
                        ipAddress = ipAddress,
                        browserInfo = browserInfo,
                        jwtExpiresInSeconds = tokenLifetime
                    )

                    val userId = user.id orDataError "Invalid user data."

                    user.lastLogin = LocalDateTime.now()
                    user.lastSeen = LocalDateTime.now()
                    user.onlineStatus = OnlineStatus.ACTIVE

                    cmsUserRepository.update(code = userId, o = user)
                    ebeanTransaction.commit()

                    LoginResponse(
                        account = user.toResponseDetail(),
                        accessToken = token,
                        permissions = accessList
                    )
                } catch (ex: Exception) {
                    log.authLog(
                        ERROR("Login user has been failed"),
                        "errors" to ex.message.toString()
                    )
                    throw ex
                }
            }
        }

        log.authLog(
            INFO("Login user has been succeed"),
            "loginTime" to loginTime
        )
        /**
         * End of core process for login user
         *
         */

        return json {
            obj(
                "data" to loginResp,
                "message" to "User was successfully logged-in."
            )
        }
    }

    suspend fun loginVerification(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val identity = body.get<String>("identity")?.trim() orBadRequest "Identity body param is required."
        val staySignedIn = body.get<Boolean>("staySignedIn") orBadRequest "Stay signed in body param is required."

        val verificationCode = body.get<String>("verificationCode")
            ?.trim() orBadRequest "Verification code body param is required."

        val registrationToken = body.get<String>("registrationToken")
            ?.trim() orBadRequest "Registration token body param is required."

        val user = cmsUserRepository.findByIdentityAnd2FACode(
            identity = identity,
            login2FACode = verificationCode
        ).findOne() orBadRequest "User email not found and/or verification code is expired. " +
                "Please contact your administrator."
        /**
         * End of validation process
         *
         */

        /**
         * Core process for login user
         *
         */
        val (loginResp, loginTime) = executeTimeMillis {

            // start DB transaction
            val ebeanTransaction = ebeanServer.beginTransaction()

            // commit & finally close DB transaction
            ebeanTransaction.use {
                try {

                    val ipAddress = context.getRemoteIpAddress()
                    val browserInfo = context.getBrowserInfo()

                    val tokenLifetime = if (staySignedIn) {
                        2592000
                    } else {
                        jwtExpiresInSeconds
                    }
                    val userRoleName = user.role.name

                    val token = jwtAuthProvider.generateToken(json {
                        obj(
                            "identity" to identity,
                            "roleName" to userRoleName
                        )
                    }, JWTOptions().apply {
                        algorithm = jwtAlgorithm
                        expiresInSeconds = tokenLifetime
                    })

                    apiKeyRepository.saveJwtTokenForUser(
                        cmsUser = user,
                        jwtToken = token,
                        registrationToken = registrationToken,
                        ipAddress = ipAddress,
                        browserInfo = browserInfo,
                        jwtExpiresInSeconds = tokenLifetime
                    )

                    val userId = user.id orDataError "Invalid user data."

                    user.lastLogin = LocalDateTime.now()
                    user.lastSeen = LocalDateTime.now()

                    user.login2FACode = null
                    user.lastSend2FACode = null
                    user.login2FACodeExpiredOn = null

                    user.lastSeen = LocalDateTime.now()
                    user.onlineStatus = OnlineStatus.ACTIVE

                    cmsUserRepository.update(code = userId, o = user)
                    ebeanTransaction.commit()

                    val accessRoles = accessRoleRepository.findAllowedAccessByRole(user.role.id).findList()
                    val allowedAccess = accessRoles.map { it.access.name }

                    LoginResponse(
                        account = user.toResponseDetail(),
                        accessToken = token,
                        permissions = allowedAccess
                    )
                } catch (ex: Exception) {
                    log.authLog(
                        ERROR("Login verification has been failed"),
                        "errors" to ex.message.toString()
                    )
                    throw ex
                }
            }
        }

        log.authLog(
            INFO("Login verification has been succeed"),
            "loginTime" to loginTime
        )
        /**
         * End of core process for login user
         *
         */

        return json {
            obj(
                "data" to loginResp,
                "message" to "User log-in was successfully verified."
            )
        }
    }

    suspend fun logout(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val user = authorizationService.authorizeUser(identity = userIdentity)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process for logout user
         *
         */
        val (_, logoutTime) = executeTimeMillis {
            try {
                val userId = user.id orDataError "Invalid user data."
                user.apiKey = null
                cmsUserRepository.update(code = userId, o = user)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Logout user has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Logout user has been succeed"),
            "logoutTime" to logoutTime
        )
        /**
         * End of core process for logout user
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "User was successfully logged-out."
            )
        }
    }

    suspend fun forgotPassword(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val email = body.get<String>("email")?.trim() orBadRequest "Email body param is required."
        val timeLimit = body.get<Boolean>("timeLimit") orBadRequest "Time limit body param is required."

        if (!email.matchWithEmail()) {
            throw ValidationException(listOf("Invalid email format. Please supply valid email address."))
        }

        val user = cmsUserRepository.findByEmail(email = email).findOne()
        val lastSendToken = user?.lastSendToken

        if (timeLimit && lastSendToken != null) {
            val timeDiff = LocalDateTime.now().toMilliSeconds() - lastSendToken.toMilliSeconds()

            if (timeDiff < TimeUnit.MINUTES.toMillis(2)) {
                throw ValidationException(
                    listOf("You can only reset your password 2 minutes after the last time you reset it."))
            }
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
                if (user != null) {
                    /**
                     * Update user's forgot code data
                     *
                     */
                    val id = user.id orDataError "Invalid user data."
                    val forgotCode = nextAlpha(19)

                    user.forgotCode = forgotCode
                    user.forgotCodeExpiredOn = LocalDateTime.now().plusHours(24)
                    user.lastSendToken = LocalDateTime.now()

                    cmsUserRepository.update(code = id, o = user)
                    /**
                     * End of update user's forgot code data
                     *
                     */

                    /**
                     * Send email for reset password to user's email
                     *
                     */
                    CoroutineScope(Dispatchers.IO).launch {

                        val fullName = user.admin.fullName
                        val subject = "Password reset instructions"

                        val emailTemplate = "templates/email-reset-password.peb"
                        val pebbleTemplate = Pebble.compileTemplate(templateLocation = emailTemplate)

                        val params = mutableMapOf(
                            "appName" to appName,
                            "fullName" to fullName,
                            "companyEmail" to companyEmail,
                            "companyPhone" to companyPhone,
                            "companyFacebookLink" to companyFacebookLink,
                            "companyInstagramLink" to companyInstagramLink,
                            "resetLink" to "$feAdminBaseUrl/reset-password?email=$email&token=$forgotCode",
                        )

                        val htmlTemplate = pebbleTemplate.evaluate(
                            parameters = params,
                            locale = Locale.ENGLISH
                        )
                        mailerService.sendMessage(
                            to = user.email,
                            subject = subject,
                            html = htmlTemplate
                        )
                    }
                    /**
                     * End of send email for reset password to user's email
                     *
                     */
                }
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Forgot password user has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Forgot password user has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "If an account exists, an email will be sent to $email " +
                        "with instructions to reset your password."
            )
        }
    }

    suspend fun resetPassword(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val email = body.get<String>("email")?.trim() orBadRequest "Email body param is required."

        val forgotCode = body.get<String>("forgotCode")?.trim() orBadRequest "Forgot code body param is required."
        val password = body.get<String>("password")?.trim() orBadRequest "Password body param is required."

        val confirmPassword = body.get<String>("confirmPassword")
            ?.trim() orBadRequest "Confirm password body param is required."

        if (password != confirmPassword) {
            throw ValidationException(listOf("Passwords do not match."))
        }
        val user = cmsUserRepository.findByEmailAndForgotCode(
            email = email,
            forgotCode = forgotCode
        ).findOne() orNotFound "Email not found or reset link has been expired. " +
                "Try to request a new link by click forgot password."

        user.password = password
        user.validate()
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

                user.password = password.encode()
                user.forgotCode = null

                cmsUserRepository.update(code = id, o = user)
            } catch (ex: Exception) {
                log.authLog(
                    ERROR("Reset password user has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.authLog(
            INFO("Reset password user has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Your new password was successfully saved. Please sign in with your new password."
            )
        }
    }

    suspend fun verifyEmail(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val email = body.get<String>("email")?.trim() orBadRequest "Email body param is required."
        val token = body.get<String>("token")?.trim() orBadRequest "Token body param is required."

        val cmsUser = cmsUserRepository.findActiveTokenByEmail(
            email = email,
            token = token
        ).findOne() orBadRequest "User email not found and/or verification link is expired. " +
                "Please contact your administrator."

        if (cmsUser.emailVerified) throw ValidationException(listOf("Your email has been verified."))
        /**
         * End of validation process
         *
         */

        /**
         * Core process for verify the user's email
         *
         */
        val (updateResp, updateTime) = executeTimeMillis {
            try {
                cmsUserRepository.updateEmailVerifiedStatus(
                    cmsUser = cmsUser,
                    email = email,
                    verifiedStatus = true
                )
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("User email verified status could not be updated to DB"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("User email verified status has been updated to DB"),
            "isUpdated" to updateResp,
            "updateTime" to updateTime
        )
        /**
         * End of core process for verify the user's email
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Your email was successfully verified."
            )
        }
    }

    suspend fun verifyPhoneNumber(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val phone = body.get<String>("phone")?.trim() orBadRequest "Phone body param is required."
        val token = body.get<String>("token")?.trim() orBadRequest "Token body param is required."

        val cmsUser = cmsUserRepository.findActiveTokenByPhone(
            phone = phone,
            tokenPhone = token
        ).findOne() orNotFound "User phone number not found and/or verification code is expired. " +
                "Try to resend verification code."
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        authorizationService.authorizeUser(identity = userIdentity, userId = cmsUser.externalId)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process for verify the user's phone number
         *
         */
        val (updateResp, updateTime) = executeTimeMillis {
            try {
                cmsUserRepository.updatePhoneVerifiedStatus(
                    cmsUser = cmsUser,
                    verifiedStatus = true
                )
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("User phone verified status could not be updated to DB"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("User phone verified status has been updated to DB"),
            "isUpdated" to updateResp,
            "updateTime" to updateTime
        )
        /**
         * End of core process for verify the user's phone number
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Your phone number was successfully verified."
            )
        }
    }

    suspend fun sendNewTokenEmail(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val email = body.get<String>("email")?.trim() orBadRequest "Email body param is required."
        val timeLimit = body.get<Boolean>("timeLimit") orBadRequest "Time limit body param is required."

        val user = cmsUserRepository.findByEmailAndVerifiedStatus(
            email = email,
            emailVerified = false
        ).findOne() orNotFound "User data not found."

        val lastSendToken = user.lastSendToken
        if (timeLimit && lastSendToken != null) {
            val timeDiff = LocalDateTime.now().toMilliSeconds() - lastSendToken.toMilliSeconds()
            if (timeDiff < TimeUnit.MINUTES.toMillis(2)) {
                throw BadRequestException("You can only send the verification link 2 minutes " +
                        "after the last time you sent it.")
            }
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
                // update user's token email
                val newToken = nextAlpha(19)

                cmsUserRepository.updateEmailAndToken(
                    cmsUser = user,
                    email = email,
                    newToken = newToken
                )

                CoroutineScope(Dispatchers.IO).launch {
                    // send new verification link to the user's email
                    val subject = "Email verification"
                    val verificationLink = "$feAdminBaseUrl/verification?email=$email&token=$newToken"

                    val emailTemplate = "templates/email-verification.peb"
                    val pebbleTemplate = Pebble.compileTemplate(templateLocation = emailTemplate)

                    val params = mutableMapOf(
                        "appName" to appName,
                        "companyEmail" to companyEmail,
                        "companyPhone" to companyPhone,
                        "companyFacebookLink" to companyFacebookLink,
                        "companyInstagramLink" to companyInstagramLink,
                        "fullName" to user.admin.fullName,
                        "verificationLink" to verificationLink
                    )
                    val htmlTemplate = pebbleTemplate.evaluate(
                        parameters = params,
                        locale = Locale.ENGLISH
                    )
                    mailerService.sendMessage(
                        to = email,
                        subject = subject,
                        html = htmlTemplate
                    )
                }
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Verification link email could not be sent"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Verification link email has been sent"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "New verification link was successfully sent to your email. Please check your email."
            )
        }
    }

    suspend fun sendNewTokenPhone(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val phone = body.get<String>("phone")?.trim() orBadRequest "Phone body param is required."
        val timeLimit = body.get<Boolean>("timeLimit") orBadRequest "Time limit body param is required."

        val user = cmsUserRepository.findByPhoneAndVerifiedStatus(
            phone = phone,
            phoneVerified = false
        ).findOne() orNotFound "User data not found."

        val lastSendTokenPhone = user.lastSendTokenPhone
        if (timeLimit && lastSendTokenPhone != null) {
            val timeDiff = LocalDateTime.now().toMilliSeconds() - lastSendTokenPhone.toMilliSeconds()
            if (timeDiff < TimeUnit.MINUTES.toMillis(2)) {
                throw BadRequestException("You can only send the verification code 2 minutes " +
                        "after the last time you sent it.")
            }
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
                // update user's token phone
                val userPhone = user.phone
                val newTokenPhone = RandomStringUtils.random(
                    6, 0, 0,
                    false, true, null,
                    Random.from(RandomGenerator.getDefault())
                )
                val tokenExpiredAt = LocalDateTime.now().plusMinutes(15)

                val userId = user.id orDataError "Invalid user data."

                user.tokenPhone = newTokenPhone
                user.lastSendTokenPhone = LocalDateTime.now()
                user.tokenPhoneExpiredOn = tokenExpiredAt

                cmsUserRepository.update(code = userId, o = user)

                CoroutineScope(Dispatchers.IO).launch {
                    // send new verification code to the user's phone number via SMS
                    if (userPhone != null) {
                        val expiredAt = tokenExpiredAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                        val textMessage = "$appName - Kode verifikasi untuk akunmu : $newTokenPhone, berlaku hingga " +
                                "$expiredAt. JANGAN BERIKAN KODE INI KE SIAPA PUN."

                        smsService.sendSMS(
                            recipient = userPhone,
                            textMessage = textMessage
                        )
                    }
                }
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Verification code SMS could not be sent"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Verification code SMS has been sent"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "New verification code was successfully sent to your phone via SMS. Please check your phone."
            )
        }
    }

    suspend fun sendNewLogin2FACode(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val identity = body.get<String>("identity")?.trim() orBadRequest "Identity body param is required."
        val timeLimit = body.get<Boolean>("timeLimit") orBadRequest "Time limit body param is required."

        val user = cmsUserRepository.findByIdentity(identity).findOne() orNotFound "User data not found."
        val lastSend2FACode = user.lastSend2FACode

        if (timeLimit && lastSend2FACode != null) {
            val timeDiff = LocalDateTime.now().toMilliSeconds() - lastSend2FACode.toMilliSeconds()
            if (timeDiff < TimeUnit.MINUTES.toMillis(2)) {
                throw BadRequestException("You can only send the verification code 2 minutes " +
                        "after the last time you sent it.")
            }
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
                // update user's login 2FA code email
                val login2FACode = RandomStringUtils.random(
                    6, 0, 0,
                    false, true, null,
                    Random.from(RandomGenerator.getDefault())
                )
                val userId = user.id orDataError "Invalid user data."

                user.login2FACode = login2FACode
                user.lastSend2FACode = LocalDateTime.now()
                user.login2FACodeExpiredOn = LocalDateTime.now().plusMinutes(60)

                cmsUserRepository.update(code = userId, o = user)

                CoroutineScope(Dispatchers.IO).launch {
                    // send new verification code to the user's email
                    val subject = "Verify your identity"
                    val emailTemplate = "templates/email-login-verification.peb"
                    val pebbleTemplate = Pebble.compileTemplate(templateLocation = emailTemplate)

                    val params = mutableMapOf(
                        "appName" to appName,
                        "companyEmail" to companyEmail,
                        "companyPhone" to companyPhone,
                        "companyFacebookLink" to companyFacebookLink,
                        "companyInstagramLink" to companyInstagramLink,
                        "fullName" to user.admin.fullName,
                        "verificationCode" to login2FACode
                    )

                    val htmlTemplate = pebbleTemplate.evaluate(
                        parameters = params,
                        locale = Locale.ENGLISH
                    )
                    mailerService.sendMessage(
                        to = user.email,
                        subject = subject,
                        html = htmlTemplate
                    )
                }
            } catch (ex: Exception) {
                log.userLog(
                    ERROR("Verification 2FA Code email could not be sent"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.userLog(
            INFO("Verification 2FA Code email has been sent"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "New verification code was successfully sent to your email. Please check your email."
            )
        }
    }
}
