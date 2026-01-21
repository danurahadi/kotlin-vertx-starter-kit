package com.starter.app.integration.fileupload.gcp

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
class GcpModule {

    @Provides
    @Singleton
    @Named("disableGcp")
    fun disableGcp(config: JsonObject): Boolean {
        val key = "DISABLE_GCP"
        return config.get<Boolean>(key) orNotFound "Enable / Disable GCP config is required"
    }

    @Provides
    @Singleton
    @Named("gcpPdfBucketName")
    fun gcpPdfBucketName(config: JsonObject): String {
        val key = "GCP_PDF_BUCKET_NAME"
        return config.get<String>(key) orNotFound "GCP pdf bucket name config is required"
    }

    @Provides
    @Singleton
    @Named("gcpDocsBucketName")
    fun gcpDocsBucketName(config: JsonObject): String {
        val key = "GCP_DOCS_BUCKET_NAME"
        return config.get<String>(key) orNotFound "GCP docs bucket name config is required"
    }

    @Provides
    @Singleton
    @Named("gcpImagesBucketName")
    fun gcpImagesBucketName(config: JsonObject): String {
        val key = "GCP_IMAGES_BUCKET_NAME"
        return config.get<String>(key) orNotFound "GCP images bucket name config is required"
    }

    @Provides
    @Singleton
    @Named("gcpCredentialsPath")
    fun gcpCredentialsPath(config: JsonObject): String {
        val key = "GCP_CREDENTIALS_PATH"
        return config.get<String>(key) orNotFound "GCP credentials path config is required"
    }
}
