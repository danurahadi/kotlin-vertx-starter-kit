package com.starter.app.integration.firebase

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
class FirebaseModule {

    @Provides
    @Singleton
    @Named("disableFirebase")
    fun disableFirebase(config: JsonObject): Boolean {
        val key = "DISABLE_FIREBASE"
        return config.get<Boolean>(key) orNotFound "Enable / Disable FIREBASE config is required"
    }

    @Provides
    @Singleton
    @Named("firebaseProjectId")
    fun firebaseProjectId(config: JsonObject): String {
        val key = "FIREBASE_PROJECT_ID"
        return config.get<String>(key) orNotFound "Firebase Project ID config is required"
    }

    @Provides
    @Singleton
    @Named("firebaseApiBaseUrl")
    fun firebaseApiBaseUrl(config: JsonObject): String {
        val key = "FIREBASE_API_BASE_URL"
        return config.get<String>(key) orNotFound "Firebase API Base URL config is required"
    }

    @Provides
    @Singleton
    @Named("firebaseServiceAccountKey")
    fun firebaseServiceAccountKey(config: JsonObject): String {
        val key = "FIREBASE_SERVICE_ACCOUNT_KEY"
        return config.get<String>(key) orNotFound "Firebase service account key path config is required"
    }

    @Provides
    @Singleton
    @Named("firebaseWebPushTtl")
    fun firebaseWebPushTtl(config: JsonObject): Long {
        val key = "FIREBASE_WEB_PUSH_TTL"
        return config.get<Long>(key) orNotFound "Firebase web push TTL config is required"
    }

    @Provides
    @Singleton
    @Named("firebaseWebPushUrgency")
    fun firebaseWebPushUrgency(config: JsonObject): String {
        val key = "FIREBASE_WEB_PUSH_URGENCY"
        return config.get<String>(key) orNotFound "Firebase web push urgency config is required"
    }
}
