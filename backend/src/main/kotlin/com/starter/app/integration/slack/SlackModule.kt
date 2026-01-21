package com.starter.app.integration.slack

import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.getExcept
import id.yoframework.core.module.CoreModule
import io.vertx.core.json.JsonObject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(includes = [CoreModule::class, EnvModule::class])
class SlackModule {

    @Provides
    @Singleton
    @Named("disableSlackApi")
    fun disableSlackApi(config: JsonObject): Boolean {
        val key = "DISABLE_SLACK_API"
        return config.getBoolean(key, false)
    }

    @Provides
    @Singleton
    @Named("slackWebhookUrl")
    fun slackWebhookUrl(config: JsonObject): String {
        val key = "SLACK_WEBHOOK_URL"
        return config.getExcept(key)
    }
}
