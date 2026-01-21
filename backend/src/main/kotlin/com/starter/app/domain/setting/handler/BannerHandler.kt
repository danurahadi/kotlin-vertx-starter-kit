package com.starter.app.domain.setting.handler

import com.starter.app.app.service.AuthorizationService
import com.starter.app.domain.setting.db.model.Banner
import com.starter.app.domain.setting.db.model.value.BannerType
import com.starter.app.domain.setting.db.repository.BannerRepository
import com.starter.app.domain.setting.settingLog
import com.starter.app.integration.fileupload.aws.AwsS3Service
import com.starter.library.extension.paginate
import com.starter.library.extension.parseFileName
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.web.exception.*
import id.yoframework.web.extension.jsonBody
import id.yoframework.web.extension.param
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
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
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class BannerHandler @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val awsS3Service: AwsS3Service,
    private val authorizationService: AuthorizationService,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String,
    @param:Named("awsBannersFolder") private val awsBannersFolder: String,
    @param:Named("bannerImageOriginalSize") private val bannerImageOriginalSize: Int
) {
    private val log = logger<BannerHandler>()

    suspend fun createBanner(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val title = body.get<String>("title")?.trim() orBadRequest "Title body param is required."
        val description = body.get<String>("description")?.trim() orBadRequest "Description body param is required."

        val linkUrl = body.get<String>("linkUrl")?.trim() orBadRequest "Link URL body param is required."
        val type = body.get<String>("type")?.trim() orBadRequest "Type body param is required."

        val base64FileString = body.get<String>("base64String")
            ?.trim() orBadRequest "Base64 String body param is required."

        val fileName = body.get<String>("fileName")?.trim() orBadRequest "File name body param is required."
        val status = body.get<Boolean>("status") orBadRequest "Status body param is required."

        if (title == "") throw ValidationException(listOf("Title could not be blank."))
        if (description == "") throw ValidationException(listOf("Description could not be blank."))

        if (linkUrl == "") throw ValidationException(listOf("Link URL could not be blank."))
        if (type == "") throw ValidationException(listOf("Type could not be blank."))

        if (base64FileString == "") throw ValidationException(listOf("Base64 String could not be blank."))
        if (fileName == "") throw ValidationException(listOf("File name could not be blank."))

        fileName.parseFileName("banner image")
        /**
         * End of validation process
         *
         */

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
        val (savedBanner, coreTime) = executeTimeMillis {
            try {
                val uploadResp = awsS3Service.uploadImageFileBase64(
                    fileName = fileName,
                    folderName = awsBannersFolder,
                    base64String = base64FileString,
                    thumbnailSize = null,
                    originalSize = bannerImageOriginalSize,
                    square = false
                )
                val banner = Banner(
                    title = title,
                    description = description,
                    linkUrl = linkUrl,
                    type = BannerType.valueOf(type),
                    image = uploadResp.originalLink,
                    status = status,
                    sequence = 0
                )
                try {
                    bannerRepository.save(banner)
                    banner
                } catch (ex: Exception) {
                    awsS3Service.deleteFiles(mediaLinks = listOf(uploadResp.originalLink))
                    throw ex
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Create banner has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Create banner has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to obj(
                    "id" to savedBanner.id.toString()
                ),
                "message" to "Create banner has been succeed."
            )
        }
    }

    suspend fun getBannerList(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val keyword = context.param("q")?.trim() orBadRequest "Keyword query param is required."
        val status = context.param("status")?.trim() orBadRequest "Status query param is required."

        val page = context.param("page")?.toInt() orBadRequest "Page query param is required."
        val limit = context.param("limit")?.toInt() orBadRequest "Limit query param is required."

        if (page < 1 || limit < 0) throw BadRequestException("Invalid pagination query params.")
        val startFrom = (page - 1) * limit

        val bannerStatus = if (status.isNotBlank()) {
            status.toBoolean()
        } else {
            null
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
                val query = bannerRepository.findBannerByStatusAndTitleContain(
                    status = bannerStatus,
                    keyword = keyword
                )
                val totalData = query.findCount()

                val s = query
                    .orderBy("sequence ASC, updatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findList()

                val paginationInfo = paginate(
                    "$apiBaseUrl/banners?q=$keyword&status=$status&",
                    page,
                    limit,
                    totalData.toLong()
                )

                s.map {
                    it.toJsonObject()
                } to paginationInfo
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get banner list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get banner list from DB has been succeed"),
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
                "message" to "Get banner list has been succeed."
            )
        }
    }

    suspend fun getLeftBanners(): JsonObject {
        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                val b = bannerRepository.findByTypeAndStatus(
                    type = BannerType.LEFT_BANNER,
                    status = true
                )
                    .orderBy("sequence ASC")
                    .findList()

                    b.map {
                        it.toJsonObject()
                    }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get left banner list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get left banner list from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Get left banner has been succeed."
            )
        }
    }

    suspend fun getRightBanners(): JsonObject {
        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                val b = bannerRepository.findByTypeAndStatus(
                    type = BannerType.RIGHT_BANNER,
                    status = true
                )
                    .orderBy("sequence ASC")
                    .findList()

                    b.map {
                        it.toJsonObject()
                    }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get right banner list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get right banner list from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Get right banner has been succeed."
            )
        }
    }

    suspend fun getBannerDetail(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id").trim().toLong() orBadRequest "Invalid banner ID path param."
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (banner, coreTime) = executeTimeMillis {
            try {
                val banner = bannerRepository.findBannerById(id).findOne() orNotFound "Banner data not found."
                banner.toJsonObjectDetail()
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get banner detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get banner detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to banner,
                "message" to "Get banner detail has been succeed."
            )
        }
    }

    suspend fun updateBanner(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id").trim().toLong() orBadRequest "Invalid banner ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val title = body.get<String>("title")?.trim() orBadRequest "Title body param is required."
        val description = body.get<String>("description")?.trim() orBadRequest "Description body param is required."

        val linkUrl = body.get<String>("linkUrl")?.trim() orBadRequest "Link URL body param is required."
        val type = body.get<String>("type")?.trim() orBadRequest "Type body param is required."

        val base64FileString = body.get<String>("base64String")?.trim()
        val fileName = body.get<String>("fileName")?.trim()
        val status = body.get<Boolean>("status") orBadRequest "Status body param is required."

        if (title == "") throw ValidationException(listOf("Title could not be blank."))
        if (description == "") throw ValidationException(listOf("Description could not be blank."))

        if (linkUrl == "") throw ValidationException(listOf("Link URL could not be blank."))
        if (type == "") throw ValidationException(listOf("Type could not be blank."))

        if (base64FileString == "") throw ValidationException(listOf("Base64 String could not be blank."))
        if (fileName == "") throw ValidationException(listOf("File name could not be blank."))

        fileName?.parseFileName("banner image")
        val banner = bannerRepository.findBannerById(id).findOne() orNotFound "Banner data not found."
        /**
         * End of validation process
         *
         */

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
        val (_, coreTime) = executeTimeMillis {
            try {
                val uploadResp = if (base64FileString != null && fileName != null) {
                    awsS3Service.uploadImageFileBase64(
                        fileName = fileName,
                        folderName = awsBannersFolder,
                        base64String = base64FileString,
                        thumbnailSize = null,
                        originalSize = bannerImageOriginalSize,
                        square = false
                    )
                } else {
                    null
                }

                banner.title = title
                banner.description = description
                banner.linkUrl = linkUrl

                banner.type = BannerType.valueOf(type)
                banner.status = status

                if (uploadResp != null) {
                    banner.image = uploadResp.originalLink
                }

                try {
                    bannerRepository.update(code = id, o = banner)
                } catch (ex: Exception) {
                    if (uploadResp != null) {
                        awsS3Service.deleteFiles(mediaLinks = listOf(uploadResp.originalLink))
                    }
                    throw ex
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Update banner by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Update banner by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to obj(
                    "id" to id.toString()
                ),
                "message" to "Update banner has been succeed."
            )
        }
    }

    suspend fun deleteBanner(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id").trim().toLong() orBadRequest "Invalid banner ID path param."
        val banner = bannerRepository.findBannerById(id).findOne() orNotFound "Banner data not found."
        val image = banner.image
        /**
         * End of validation process
         *
         */

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
        val (_, coreTime) = executeTimeMillis {
            try {
                bannerRepository.delete(banner)

                // Delete the old banner from Cloud Storage
                if (image != null) {
                    awsS3Service.deleteFiles(listOf(image))
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Delete banner by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Delete banner by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Delete banner has been succeed."
            )
        }
    }
}
