package com.starter.app.integration.fileupload.aws

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
class AwsS3Module {

    @Provides
    @Singleton
    @Named("disableAwsS3")
    fun disableAwsS3(config: JsonObject): Boolean {
        val key = "DISABLE_AWS_S3"
        return config.get<Boolean>(key) orNotFound "Enable / Disable AWS S3 config is required"
    }

    @Provides
    @Singleton
    @Named("awsAccessKey")
    fun awsAccessKey(config: JsonObject): String {
        val key = "AWS_S3_ACCESS_KEY"
        return config.get<String>(key) orNotFound "AWS S3 access key config is required"
    }

    @Provides
    @Singleton
    @Named("awsSecretKey")
    fun awsSecretKey(config: JsonObject): String {
        val key = "AWS_S3_SECRET_KEY"
        return config.get<String>(key) orNotFound "AWS S3 secret key config is required"
    }

    @Provides
    @Singleton
    @Named("awsBaseUrl")
    fun awsBaseUrl(config: JsonObject): String {
        val key = "AWS_S3_BASE_URL"
        return config.get<String>(key) orNotFound "AWS S3 Base URL config is required"
    }

    @Provides
    @Singleton
    @Named("awsAliasUrl")
    fun awsAliasUrl(config: JsonObject): String {
        val key = "AWS_S3_ALIAS_URL"
        return config.get<String>(key) orNotFound "AWS S3 Alias URL config is required"
    }

    @Provides
    @Singleton
    @Named("awsRegionName")
    fun awsRegionName(config: JsonObject): String {
        val key = "AWS_S3_REGION_NAME"
        return config.get<String>(key) orNotFound "AWS S3 Region Name config is required"
    }

    @Provides
    @Singleton
    @Named("awsBucketName")
    fun awsBucketName(config: JsonObject): String {
        val key = "AWS_S3_BUCKET_NAME"
        return config.get<String>(key) orNotFound "AWS S3 Bucket Name config is required"
    }

    @Provides
    @Singleton
    @Named("awsAvatarFolder")
    fun awsAvatarFolder(config: JsonObject): String {
        val key = "AWS_S3_AVATAR_FOLDER"
        return config.get<String>(key) orNotFound "AWS S3 Avatar Folder config is required"
    }

    @Provides
    @Singleton
    @Named("awsBannersFolder")
    fun awsBannersFolder(config: JsonObject): String {
        val key = "AWS_S3_BANNERS_FOLDER"
        return config.get<String>(key) orNotFound "AWS S3 Banners Folder config is required"
    }

    @Provides
    @Singleton
    @Named("awsLogoFolder")
    fun awsLogoFolder(config: JsonObject): String {
        val key = "AWS_S3_LOGO_FOLDER"
        return config.get<String>(key) orNotFound "AWS S3 Logo Folder config is required"
    }
}
