package com.starter.app.integration.eventbus

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
class EventBusModule {

    @Provides
    @Singleton
    @Named("disableEventBusService")
    fun disableEventBusService(config: JsonObject): Boolean {
        val key = "DISABLE_EVENTBUS_SERVICE"
        return config.getBoolean(key, false)
    }

    @Provides
    @Singleton
    @Named("eventBusSendTimeout")
    fun eventBusSendTimeout(config: JsonObject): Long {
        val key = "EVENTBUS_SEND_TIMEOUT"
        return config.get<Long>(key) orDataError "Event bus send timeout config is required"
    }
}
