package com.starter.app.domain.admin.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.admin.adminLog
import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.admin.db.repository.AdminRepository
import com.starter.app.domain.auth.db.repository.RoleRepository
import com.starter.app.domain.notification.db.model.value.NotificationStatus
import com.starter.app.domain.notification.db.repository.AdminNotificationRepository
import com.starter.app.domain.user.db.model.CmsUser
import com.starter.app.domain.user.db.model.CmsUserSetting
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import com.starter.app.domain.user.db.repository.CmsUserRepository
import com.starter.app.integration.fileupload.aws.AwsS3Service
import com.starter.app.integration.mailer.MailerService
import com.starter.library.extension.paginate
import com.starter.library.extension.parseImageFileName
import com.starter.library.extension.validate
import com.starter.library.extension.validateUnique
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.extra.extension.password.encode
import id.yoframework.extra.extension.pebble.Pebble
import id.yoframework.extra.extension.pebble.compileTemplate
import id.yoframework.extra.extension.pebble.evaluate
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.InvalidCredentials
import id.yoframework.web.exception.ValidationException
import id.yoframework.web.exception.orBadRequest
import id.yoframework.web.exception.orDataError
import id.yoframework.web.exception.orNotFound
import id.yoframework.web.extension.jsonBody
import id.yoframework.web.extension.param
import io.ebean.Database
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Handler class for manage [Admin] data and get some statistic data for dashboard
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AdminHandler @Inject constructor(
    private val adminRepository: AdminRepository,
    private val roleRepository: RoleRepository,
    private val cmsUserRepository: CmsUserRepository,
    private val adminNotificationRepository: AdminNotificationRepository,
    private val awsS3Service: AwsS3Service,
    private val ebeanDatabase: Database,
    private val authorizationService: AuthorizationService,
    private val mailerService: MailerService,
    @param:Named("appName") private val appName: String,
    @param:Named("companyEmail") private val companyEmail: String,
    @param:Named("companyPhone") private val companyPhone: String,
    @param:Named("companyFacebookLink") private val companyFacebookLink: String,
    @param:Named("companyInstagramLink") private val companyInstagramLink: String,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String,
    @param:Named("awsAvatarFolder") private val awsAvatarFolder: String,
    @param:Named("avatarThumbnailSize") private val avatarThumbnailSize: Int,
    @param:Named("avatarOriginalSize") private val avatarOriginalSize: Int,
    @param:Named("feAdminBaseUrl") private val feAdminBaseUrl: String
) {
    private val log = logger<AdminHandler>()

    suspend fun createAdmin(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInUser = authorizationService.authorizeUser(identity = userIdentity)
        /**
         * End of authentication process
         *
         */

        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val email = body.get<String>("email")?.trim() orBadRequest "Email body param is required."
        val username = body.get<String>("username")?.trim() orBadRequest "Username body param is required."

        val password = body.get<String>("password")?.trim() orBadRequest "Password body param is required."
        val passwordConfirm = body.get<String>("passwordConfirm")?.trim() orBadRequest "Password confirm body param is required."

        val phone = body.get<String>("phone")?.trim()
        val fullName = body.get<String>("fullName")?.trim() orBadRequest "Fullname body param is required."
        val roleId = body.get<String>("roleId")?.trim() orBadRequest "Role ID body param is required."

        if (password != passwordConfirm) {
            throw ValidationException(listOf("Passwords do not match."))
        }

        // get role from DB
        val role = roleRepository.findByExternalId(roleId)
            .findOne() orNotFound "Role data not found."

        val cmsUser = CmsUser(
            role = role,
            email = email,
            username = username,
            phone = phone,
            password = password,
            emailVerified = true,
            status = CmsUserStatus.ACTIVE
        )
        val admin = Admin(
            cmsUser = cmsUser,
            fullName = fullName,
            superadmin = role.name == "SUPERADMIN",
        )
        val userSetting = CmsUserSetting(
            cmsUser = cmsUser,
            timezoneOffset = 420
        )

        // validate user object
        cmsUser.validate()
        cmsUser.validateUnique(ebeanDatabase)

        // validate admin object
        admin.validate()
        /**
         * End of validation process
         *
         */

        /**
         * Core process for create new User & Admin Profile
         *
         */
        val (_, coreTime) = executeTimeMillis {
            try {
                // set hashed password to user object
                cmsUser.password = password.encode()

                // set user's created by & last updated by
                cmsUser.setting = userSetting
                cmsUser.createdBy = loggedInUser
                cmsUser.lastUpdatedBy = loggedInUser

                // insert user data
                cmsUserRepository.insert(cmsUser)

                // insert admin data
                adminRepository.insert(admin)
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("User & Admin Profile data could not be saved to DB"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("User & Admin Profile data has been saved to DB"),
            "coreTime" to coreTime
        )
        /**
         * End of core process for create new User & Profile
         *
         */

        /**
         * Send email to user about the newly created account and the credentials
         *
         */
        val (_, emailTime) = executeTimeMillis {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val emailTemplate = "templates/email-user-activation.peb"
                    val pebbleTemplate = Pebble.compileTemplate(templateLocation = emailTemplate)

                    val subject = "Your new account is here"
                    val message = "Your Starter App account just have been created."

                    val params = mutableMapOf(
                        "signInUrl" to "$feAdminBaseUrl/login",
                        "appName" to appName,
                        "fullName" to fullName,
                        "email" to email,
                        "username" to username,
                        "password" to password,
                        "message" to message,
                        "companyEmail" to companyEmail,
                        "companyPhone" to companyPhone,
                        "companyFacebookLink" to companyFacebookLink,
                        "companyInstagramLink" to companyInstagramLink,
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
                log.adminLog(
                    ERROR("User activation email could not be sent"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("User activation email has been sent"),
            "emailTime" to emailTime
        )
        /**
         * End of send email to user about the newly created account and the credentials
         *
         */

        return json {
            obj(
                "data" to admin.toResponseDetail(),
                "message" to "Admin was successfully created. The username & password have been sent to the registered email."
            )
        }
    }

    suspend fun getAdminList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val statusStr = context.param("status")?.trim() orBadRequest "Status query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 1) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val status = if (statusStr != "") {
            try {
                CmsUserStatus.valueOf(statusStr)
            } catch (_: Exception) {
                throw BadRequestException("Invalid status query param.")
            }
        } else null
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create query for get admin list
                val query = adminRepository.findByStatusAndNameContain(
                    status = status,
                    keyword = keyword,
                )

                // execute query against DB
                val pagedList = query
                    .orderBy("createdAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findPagedList()

                // load total rows in background thread
                pagedList.loadCount()

                // get admin list
                val admins = pagedList.list

                // get total rows from the future
                val totalData = pagedList.totalCount

                val paginationInfo = paginate(
                    "$apiBaseUrl/admins?q=$keyword&status=$status&",
                    page,
                    limit,
                    totalData.toLong()
                )

                admins.map { it.toResponseCompact() } to paginationInfo
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("Get admin list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("Get admin list from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data.first,
                "pagination" to data.second,
                "message" to "Admin list was successfully fetched."
            )
        }
    }

    suspend fun getProfile(context: RoutingContext): JsonObject {
        /**
         * Authorization process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity)

        val adminId = context.pathParam("id")?.trim() orBadRequest "Invalid admin ID path param."
        val admin = adminRepository.findByExternalId(adminId).findOne() orNotFound "Admin data not found."

        if (loggedInAdmin.id != admin.id) {
            throw InvalidCredentials("You are not authorized to access this feature.")
        }
        /**
         * End of authorization process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                val unreadNotificationsCount = adminNotificationRepository.findByAdminIdAndStatus(
                    adminId = admin.id,
                    status = NotificationStatus.NEW
                ).findCount()

                // transform to DTO class
                admin.toResponseDetail(unreadNotificationsCount)
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("Get admin detail by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("Get admin detail by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Admin info was successfully fetched."
            )
        }
    }

    suspend fun getGeneralInfoDashboard(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        authorizationService.authorizeAdmin(identity = userIdentity)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                null
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("Get admin dashboard general info has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("Get admin dashboard general info has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Admin dashboard general info was successfully fetched."
            )
        }
    }

    suspend fun updateProfile(context: RoutingContext): JsonObject {
        /**
         * Authentication process
         *
         */
        val externalId = context.pathParam("id")?.trim() orBadRequest "Invalid admin ID path param."
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity, adminId = externalId)
        /**
         * End of authentication process
         *
         */

        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val fullName = body.get<String>("fullName")?.trim() orBadRequest "Fullname body param is required."

        // update admin profile data
        loggedInAdmin.fullName = fullName
        loggedInAdmin.complete = true

        // validate admin Object
        loggedInAdmin.validate()
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
                val id = loggedInAdmin.id orDataError "Invalid admin data."
                adminRepository.update(code = id, o = loggedInAdmin)
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("Update admin profile has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("Update admin profile has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to loggedInAdmin.toResponseDetail(),
                "message" to "Profile was successfully updated."
            )
        }
    }

    suspend fun uploadProfilePicture(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val externalId = context.pathParam("id").trim() orBadRequest "Invalid admin ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val base64FileString = body.get<String>("base64String")?.trim() orBadRequest "Base64 String body param is required."
        val fileName = body.get<String>("fileName")?.trim() orBadRequest "File name body param is required."

        if (base64FileString == "") throw ValidationException(listOf("Base64 String could not be blank."))
        if (fileName == "") throw ValidationException(listOf("File name could not be blank."))

        fileName.parseImageFileName(type = "profile picture")
        /**
         * End of validation process
         *
         */

        /**
         * Authentication process
         *
         */
        val userIdentity = context.get<String>("identity") orDataError "Invalid user data."
        val loggedInAdmin = authorizationService.authorizeAdmin(identity = userIdentity, adminId = externalId)
        /**
         * End of authentication process
         *
         */

        /**
         * Core process
         *
         */
        val (uploadResp, coreTime) = executeTimeMillis {
            try {
                val thumbnailProfileImage = loggedInAdmin.thumbnailProfileImage
                val originalProfileImage = loggedInAdmin.originalProfileImage

                val uploadResp = awsS3Service.uploadImageFileBase64(
                    fileName = fileName,
                    folderName = awsAvatarFolder,
                    base64String = base64FileString,
                    thumbnailSize = avatarThumbnailSize,
                    originalSize = avatarOriginalSize,
                    square = false
                )

                loggedInAdmin.thumbnailProfileImage = uploadResp.thumbnailLink
                loggedInAdmin.originalProfileImage = uploadResp.originalLink

                val id = loggedInAdmin.id orDataError "Invalid admin data."
                adminRepository.update(code = id, o = loggedInAdmin)

                // Delete the old avatar (thumbnail & original file) from Cloud Storage
                coroutineScope {
                    launch {
                        thumbnailProfileImage?.let { tf ->
                            originalProfileImage?.let { of ->
                                awsS3Service.deleteFiles(listOf(tf, of))
                            }
                        }
                    }
                }

                uploadResp
            } catch (ex: Exception) {
                log.adminLog(
                    ERROR("Upload admin profile image has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.adminLog(
            INFO("Upload admin profile image has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to uploadResp,
                "message" to "Profile picture was successfully uploaded."
            )
        }
    }
}
