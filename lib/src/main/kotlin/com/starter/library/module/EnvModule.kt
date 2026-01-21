package com.starter.library.module

import dagger.Module
import dagger.Provides
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.json.getExcept
import id.yoframework.core.module.CoreModule
import io.vertx.core.json.JsonObject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 */

@Module(includes = [CoreModule::class])
class EnvModule {

    private val log = logger(EnvModule::class)

    @Provides
    @Singleton
    @Named("corsAllowedOrigin")
    fun corsAllowedOrigin(config: JsonObject): String {
        return try {
            val corsAllowedOriginPattern = config.getExcept<String>("CORS_ALLOWED_ORIGIN_PATTERN")
            log.info("Initialize CORS_ALLOWED_ORIGIN_PATTERN with value $corsAllowedOriginPattern")
            corsAllowedOriginPattern
        } catch (e: Exception) {
            log.error("Initialize CORS_ALLOWED_ORIGIN_PATTERN failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("feAdminBaseUrl")
    fun feAdminBaseUrl(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("FE_ADMIN_BASE_URL")
            log.info("Initialize FE_ADMIN_BASE_URL with value $url")
            url
        } catch (e: Exception) {
            log.error("Initialize FE_ADMIN_BASE_URL failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("apiBaseUrl")
    fun apiBaseUrl(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("API_BASE_URL")
            log.info("Initialize API_BASE_URL with value $url")
            url
        } catch (e: Exception) {
            log.error("Initialize API_BASE_URL failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("aesEncryptionToken")
    fun aesEncryptionToken(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("AES_ENCRYPTION_TOKEN")
            url
        } catch (e: Exception) {
            log.error("Initialize AES_ENCRYPTION_TOKEN failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("appName")
    fun appName(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("APP_NAME")
            url
        } catch (e: Exception) {
            log.error("Initialize APP_NAME failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("companyEmail")
    fun companyEmail(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("COMPANY_EMAIL")
            url
        } catch (e: Exception) {
            log.error("Initialize COMPANY_EMAIL failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("companyPhone")
    fun companyPhone(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("COMPANY_PHONE")
            url
        } catch (e: Exception) {
            log.error("Initialize COMPANY_PHONE failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("companyFacebookLink")
    fun companyFacebookLink(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("COMPANY_FACEBOOK_LINK")
            url
        } catch (e: Exception) {
            log.error("Initialize COMPANY_FACEBOOK_LINK failed ${e.message}", e)
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("companyInstagramLink")
    fun companyInstagramLink(config: JsonObject): String {
        return try {
            val url = config.getExcept<String>("COMPANY_INSTAGRAM_LINK")
            url
        } catch (e: Exception) {
            log.error("Initialize COMPANY_INSTAGRAM_LINK failed ${e.message}", e)
            throw e
        }
    }
}
