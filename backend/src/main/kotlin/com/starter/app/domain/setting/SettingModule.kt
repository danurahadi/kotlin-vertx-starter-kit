package com.starter.app.domain.setting

import com.starter.library.module.EBeanModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.get
import id.yoframework.web.exception.orNotFound
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Named
import javax.inject.Singleton

/**
 * Class that represent Setting Module and will provide some value, such as from config file.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@Module(
    includes = [
        EBeanModule::class
    ]
)
class SettingModule {

    @Provides
    @Singleton
    @Named("fileRetentionPeriod")
    fun fileRetentionPeriod(config: JsonObject): Long {
        val key = "UPLOADED_FILE_RETENTION_PERIOD_IN_HOURS"
        return config.get<Long>(key) orNotFound "Uploaded file retention period config is required."
    }
}
