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
import id.yoframework.web.exception.BadRequestException
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
class SlideshowHandler @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val awsS3Service: AwsS3Service,
    private val authorizationService: AuthorizationService,
    private val ebeanServer: Database,
    @param:Named("apiBaseUrl") private val apiBaseUrl: String,
    @param:Named("awsBannersFolder") private val awsBannersFolder: String,
    @param:Named("slideshowImageOriginalSize") private val slideshowImageOriginalSize: Int
) {
    private val log = logger<SlideshowHandler>()

    suspend fun createSlideshow(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val title = body.get<String>("title")?.trim() orBadRequest "Title body param is required."

        val description = body.get<String>("description")?.trim() orBadRequest "Description body param is required."
        val linkUrl = body.get<String>("linkUrl")?.trim() orBadRequest "Link URL body param is required."

        val base64FileString = body.get<String>("base64String")
            ?.trim() orBadRequest "Base64 String body param is required."

        val fileName = body.get<String>("fileName")?.trim() orBadRequest "File name body param is required."
        val status = body.get<Boolean>("status") orBadRequest "Status body param is required."

        if (title == "") throw ValidationException(listOf("Title could not be blank."))
        if (description == "") throw ValidationException(listOf("Description could not be blank."))
        if (linkUrl == "") throw ValidationException(listOf("Link URL could not be blank."))

        if (base64FileString == "") throw ValidationException(listOf("Base64 String could not be blank."))
        if (fileName == "") throw ValidationException(listOf("File name could not be blank."))

        fileName.parseFileName("slideshow image")
        val slideshowQuery = bannerRepository.findByTypeAndStatus(type = BannerType.HOMEPAGE_SLIDESHOW, status = true)
        val slideshowCount = slideshowQuery.findCount()

        val newSequence = if (status) {
            slideshowCount.inc()
        } else {
            0
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
        val (savedSlideshow, coreTime) = executeTimeMillis {
            try {
                val uploadResp = awsS3Service.uploadImageFileBase64(
                    fileName = fileName,
                    folderName = awsBannersFolder,
                    base64String = base64FileString,
                    thumbnailSize = null,
                    originalSize = slideshowImageOriginalSize,
                    square = false
                )
                val slideshow = Banner(
                    title = title,
                    description = description,
                    linkUrl = linkUrl,
                    type = BannerType.HOMEPAGE_SLIDESHOW,
                    sequence = newSequence,
                    image = uploadResp.originalLink,
                    status = status
                )
                try {
                    bannerRepository.save(slideshow)
                    slideshow
                } catch (ex: Exception) {
                    awsS3Service.deleteFiles(mediaLinks = listOf(uploadResp.originalLink))
                    throw ex
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Create slideshow has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Create slideshow has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to obj(
                    "id" to savedSlideshow.id.toString()
                ),
                "message" to "Create slideshow has been succeed."
            )
        }
    }

    suspend fun getSlideshowList(context: RoutingContext): JsonObject {
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

        val slideshowStatus = if (status.isNotBlank()) {
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
                val query = bannerRepository.findSlideshowByStatusAndTitleContain(
                    status = slideshowStatus,
                    keyword = keyword
                )
                val totalData = query.findCount()

                val s = query
                    .orderBy("sequence ASC, updatedAt DESC")
                    .setFirstRow(startFrom)
                    .setMaxRows(limit)
                    .findList()

                val paginationInfo = paginate(
                    "$apiBaseUrl/slideshow?q=$keyword&status=$status&",
                    page,
                    limit,
                    totalData.toLong()
                )

                s.map {
                    it.toJsonObject()
                } to paginationInfo
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get slideshow list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get slideshow list from DB has been succeed"),
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
                "message" to "Get slideshow list has been succeed."
            )
        }
    }

    suspend fun getSlideshowLanding(): JsonObject {
        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                val b = bannerRepository.findByTypeAndStatus(
                    type = BannerType.HOMEPAGE_SLIDESHOW,
                    status = true
                )
                    .orderBy("sequence ASC")
                    .findList()

                b.map {
                    it.toJsonObject()
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get homepage slideshow list from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get homepage slideshow list from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "Get landing slideshow has been succeed."
            )
        }
    }

    suspend fun getSlideshowDetail(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim()?.toLong() orBadRequest "Invalid slideshow ID path param."
        /**
         * End of validation process
         *
         */

        /**
         * Core process
         *
         */
        val (slideshow, coreTime) = executeTimeMillis {
            try {
                val slideshow = bannerRepository.findSlideshowById(id).findOne() orNotFound "Slideshow data not found."
                slideshow.toJsonObjectDetail()
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get slideshow detail by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get slideshow detail by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to slideshow,
                "message" to "Get slideshow detail has been succeed."
            )
        }
    }

    suspend fun updateSlideshow(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim()?.toLong() orBadRequest "Invalid slideshow ID path param."
        val body = context.jsonBody() orBadRequest "Invalid JSON request body."

        val title = body.get<String>("title")?.trim() orBadRequest "Title body param is required."
        val description = body.get<String>("description")?.trim() orBadRequest "Description body param is required."
        val linkUrl = body.get<String>("linkUrl")?.trim() orBadRequest "Link URL body param is required."

        val base64FileString = body.get<String>("base64String")?.trim()
        val fileName = body.get<String>("fileName")?.trim()
        val status = body.get<Boolean>("status") orBadRequest "Status body param is required."

        if (title == "") throw ValidationException(listOf("Title could not be blank."))
        if (description == "") throw ValidationException(listOf("Description could not be blank."))
        if (linkUrl == "") throw ValidationException(listOf("Link URL could not be blank."))

        if (base64FileString == "") throw ValidationException(listOf("Base64 String could not be blank."))
        if (fileName == "") throw ValidationException(listOf("File name could not be blank."))

        fileName?.parseFileName("slideshow image")
        val slideshow = bannerRepository.findSlideshowById(id).findOne() orNotFound "Slideshow data not found."

        val slideshowQuery = bannerRepository.findByTypeAndStatus(type = BannerType.HOMEPAGE_SLIDESHOW, status = true)
        val slideshowCount = slideshowQuery.findCount()
        val previousSequence = slideshow.sequence

        val newSequence = if (status) {
            slideshowCount.inc()
        } else {
            0
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
        val (_, coreTime) = executeTimeMillis {
            try {
                val transaction = ebeanServer.beginTransaction()
                val oldImage = slideshow.image

                try {
                    val uploadResp = if (base64FileString != null && fileName != null) {
                        awsS3Service.uploadImageFileBase64(
                            fileName = fileName,
                            folderName = awsBannersFolder,
                            base64String = base64FileString,
                            thumbnailSize = null,
                            originalSize = slideshowImageOriginalSize,
                            square = false
                        )
                    } else {
                        null
                    }

                    slideshow.title = title
                    slideshow.description = description

                    slideshow.linkUrl = linkUrl
                    slideshow.sequence = newSequence
                    slideshow.status = status

                    if (uploadResp != null) {
                        slideshow.image = uploadResp.originalLink
                    }

                    try {
                        bannerRepository.update(code = id, o = slideshow, transaction = transaction)

                        if (!status && previousSequence != slideshowCount) {
                            val publishedSlideshow = slideshowQuery.orderBy("sequence ASC").findList()

                            val updatedSlideshow = publishedSlideshow.mapIndexed { i, b ->
                                b.sequence = i.inc()
                                b
                            }

                            bannerRepository.updateAll(list = updatedSlideshow, transaction = transaction)
                        }

                        if (uploadResp != null && oldImage != null) {
                            awsS3Service.deleteFiles(mediaLinks = listOf(oldImage))
                        }
                        transaction.commit()
                    } catch (ex: Exception) {
                        if (uploadResp != null) {
                            awsS3Service.deleteFiles(mediaLinks = listOf(uploadResp.originalLink))
                        }
                        throw ex
                    }
                } finally {
                    transaction.end()
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Update slideshow by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Update slideshow by ID has been succeed"),
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
                "message" to "Update slideshow has been succeed."
            )
        }
    }

    suspend fun updateSequence(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val slideshowId = context.pathParam("id")?.trim()
            ?.toLong() orBadRequest "Invalid slideshow ID path param."

        val body = context.jsonBody() orBadRequest "Invalid JSON request body."
        val newOrder = body.get<Int>("newOrder") orBadRequest "New order body param is required."

        val slideshow = bannerRepository.findSlideshowById(id = slideshowId)
            .findOne() orNotFound "Slideshow data not found."

        val previousSequence = slideshow.sequence
        val slideshowQuery = bannerRepository.findByTypeAndStatus(type = BannerType.HOMEPAGE_SLIDESHOW, status = true)
        val slideshowCount = slideshowQuery.findCount()

        if (newOrder !in 1..slideshowCount) {
            throw BadRequestException("Invalid new order body param.")
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
        val (_, coreTime) = executeTimeMillis {
            try {
                if (newOrder != previousSequence) {
                    val otherSlideshow = slideshowQuery.findList().filter {
                        it.id != slideshowId
                    }
                        .map { s ->
                            val seq = s.sequence

                            if (newOrder > previousSequence) {
                                if (seq in previousSequence..newOrder) {
                                    s.sequence -= 1
                                }
                            } else {
                                if (seq in newOrder..previousSequence) {
                                    s.sequence += 1
                                }
                            }

                            s
                        }

                    slideshow.sequence = newOrder

                    val transaction = ebeanServer.beginTransaction()
                    try {
                        bannerRepository.update(code = slideshowId, o = slideshow)
                        bannerRepository.updateAll(otherSlideshow)

                        transaction.commit()
                    } finally {
                        transaction.end()
                    }
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Update slideshow sequence by ID has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Update slideshow sequence by ID has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to obj(
                    "id" to slideshowId.toString()
                ),
                "message" to "Update slideshow sequence has been succeed."
            )
        }
    }

    suspend fun deleteSlideshow(context: RoutingContext): JsonObject {
        /**
         * Validation process
         *
         */
        val id = context.pathParam("id")?.trim()?.toLong() orBadRequest "Invalid slideshow ID path param."
        val slideshow = bannerRepository.findSlideshowById(id).findOne() orNotFound "Slideshow data not found."
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
                bannerRepository.delete(slideshow)

                // Delete the old slideshow from Cloud Storage
                val image = slideshow.image
                if (image != null) {
                    awsS3Service.deleteFiles(listOf(image))
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Delete slideshow by ID from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Delete slideshow by ID from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to null,
                "message" to "Delete slideshow has been succeed."
            )
        }
    }
}
