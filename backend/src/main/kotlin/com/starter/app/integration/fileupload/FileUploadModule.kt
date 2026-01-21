package com.starter.app.integration.fileupload

import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.get
import id.yoframework.web.exception.orNotFound
import io.vertx.core.json.JsonObject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(includes = [EnvModule::class])
class FileUploadModule {

    @Provides
    @Singleton
    @Named("maxFileSize")
    fun maxFileSize(config: JsonObject): Int {
        val key = "MAX_FILE_SIZE_IN_MB"
        return config.get<Int>(key) orNotFound "Max file size (in MB) config is required"
    }

    @Provides
    @Singleton
    @Named("maxImageFileSize")
    fun maxImageFileSize(config: JsonObject): Int {
        val key = "MAX_IMAGE_FILE_SIZE_IN_MB"
        return config.get<Int>(key) orNotFound "Max image file size (in MB) config is required"
    }

    @Provides
    @Singleton
    @Named("avatarThumbnailSize")
    fun avatarThumbnailSize(config: JsonObject): Int {
        val key = "AVATAR_THUMBNAIL_SIZE_IN_PIXEL"
        return config.get<Int>(key) orNotFound "Avatar thumbnail size (in Pixel) config is required"
    }

    @Provides
    @Singleton
    @Named("avatarOriginalSize")
    fun avatarOriginalSize(config: JsonObject): Int {
        val key = "AVATAR_ORIGINAL_SIZE_IN_PIXEL"
        return config.get<Int>(key) orNotFound "Avatar original size (in Pixel) config is required"
    }

    @Provides
    @Singleton
    @Named("bannerImageOriginalSize")
    fun bannerImageOriginalSize(config: JsonObject): Int {
        val key = "BANNER_IMAGE_ORIGINAL_SIZE_IN_PIXEL"
        return config.get<Int>(key) orNotFound "Banner image original size (in Pixel) config is required"
    }

    @Provides
    @Singleton
    @Named("slideshowImageOriginalSize")
    fun slideshowImageOriginalSize(config: JsonObject): Int {
        val key = "SLIDESHOW_IMAGE_ORIGINAL_SIZE_IN_PIXEL"
        return config.get<Int>(key) orNotFound "Slideshow image original size (in Pixel) config is required"
    }
}
