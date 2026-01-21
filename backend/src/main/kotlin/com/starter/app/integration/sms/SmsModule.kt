package com.starter.app.integration.sms

import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.get
import id.yoframework.web.exception.orDataError
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
class SmsModule {

    @Provides
    @Singleton
    @Named("disableSms")
    fun disableSms(config: JsonObject): Boolean {
        val key = "DISABLE_TWILIO_API"
        return config.get<Boolean>(key) orDataError "$key config is required"
    }

    @Provides
    @Singleton
    @Named("twilioAccountSid")
    fun twilioAccountSid(config: JsonObject): String {
        val key = "TWILIO_ACCOUNT_SID"
        return config.get<String>(key) orDataError "$key config is required"
    }

    @Provides
    @Singleton
    @Named("twilioAuthToken")
    fun twilioAuthToken(config: JsonObject): String {
        val key = "TWILIO_AUTH_TOKEN"
        return config.get<String>(key) orDataError "$key config is required"
    }

    @Provides
    @Singleton
    @Named("twilioRegion")
    fun twilioRegion(config: JsonObject): String {
        val key = "TWILIO_REGION"
        return config.get<String>(key) orDataError "$key config is required"
    }

    @Provides
    @Singleton
    @Named("twilioEdge")
    fun twilioEdge(config: JsonObject): String {
        val key = "TWILIO_EDGE"
        return config.get<String>(key) orDataError "$key config is required"
    }

    @Provides
    @Singleton
    @Named("twilioSenderName")
    fun twilioSenderName(config: JsonObject): String {
        val key = "TWILIO_SENDER_NAME"
        return config.get<String>(key) orDataError "$key config is required"
    }

    @Provides
    @Singleton
    @Named("twilioSenderPhoneNumber")
    fun twilioSenderPhoneNumber(config: JsonObject): String {
        val key = "TWILIO_SENDER_PHONE_NUMBER"
        return config.get<String>(key) orDataError "$key config is required"
    }
}
