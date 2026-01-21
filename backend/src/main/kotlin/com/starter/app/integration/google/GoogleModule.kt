package com.starter.app.integration.google

import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.get
import id.yoframework.web.exception.orNotFound
import io.vertx.core.json.JsonObject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Configurable values / object for Google API integration
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(includes = [EnvModule::class])
class GoogleModule {

    @Provides
    @Singleton
    @Named("disableGoogleApi")
    fun disableGoogleApi(config: JsonObject): Boolean {
        val key = "DISABLE_GOOGLE_API"
        return config.getBoolean(key, false)
    }

    @Provides
    @Singleton
    @Named("googleApiClientId")
    fun googleApiClientId(config: JsonObject): String {
        val key = "GOOGLE_API_CLIENT_ID"
        return config.get<String>(key) orNotFound "Google API Client ID config is required."
    }

    @Provides
    @Singleton
    @Named("googleApiClientSecret")
    fun googleApiClientSecret(config: JsonObject): String {
        val key = "GOOGLE_API_CLIENT_SECRET"
        return config.get<String>(key) orNotFound "Google API Client Secret config is required."
    }

    @Provides
    @Singleton
    @Named("googleBaseUrl")
    fun googleBaseUrl(config: JsonObject): String {
        val key = "GOOGLE_BASE_URL"
        return config.get<String>(key) orNotFound "Google Base URL config is required."
    }

    @Provides
    @Singleton
    @Named("googleRecaptchaServerKey")
    fun googleRecaptchaServerKey(config: JsonObject): String {
        val key = "GOOGLE_RECAPTCHA_SERVER_KEY"
        return config.get<String>(key) orNotFound "Google Recaptcha Server Key config is required."
    }
}
