package com.starter.app.scheduler

import com.starter.app.domain.setting.handler.TempFileHandler
import com.starter.app.domain.user.handler.CmsUserHandler
import com.starter.app.scheduler.job.AutoUnlockUserJob
import com.starter.app.scheduler.job.TempFileJob
import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.get
import id.yoframework.quartz.cronTrigger
import id.yoframework.quartz.job
import id.yoframework.web.exception.orNotFound
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.quartz.CronTrigger
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(includes = [EnvModule::class])
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class SchedulerModule {

    @Provides
    @Singleton
    @Named("disableScheduler")
    fun disableScheduler(config: JsonObject): Boolean {
        val key = "DISABLE_SCHEDULER"
        return config.get<Boolean>(key) orNotFound "Enable / Disable Scheduler config is required."
    }

    @Provides
    @Singleton
    @Named("tempFileJobTrigger")
    fun tempFileJobTrigger(config: JsonObject): CronTrigger? {
        val cronKey = "TEMP_FILE_CRON_EXPRESSION"
        val cronExpression = config.get<String>(cronKey) orNotFound "Temp file cron expression is required."
        return cronTrigger(cronExpression).build()
    }

    @Provides
    @Singleton
    @Named("tempFileJobScheduler")
    fun tempFileJobScheduler(
        @Named("tempFileJobTrigger") tempFileJobTrigger: CronTrigger?,
        tempFileHandler: TempFileHandler
    ): Scheduler {
        val tempFileJob = job<TempFileJob>(
            "tempFileHandler" to tempFileHandler
        ).build()

        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.scheduleJob(tempFileJob, tempFileJobTrigger)

        return scheduler
    }

    @Provides
    @Singleton
    @Named("autoUnlockUserJobTrigger")
    fun autoUnlockUserJobTrigger(config: JsonObject): CronTrigger? {
        val cronKey = "AUTO_UNLOCK_USER_CRON_EXPRESSION"
        val cronExpression = config.get<String>(cronKey) orNotFound "Auto unlock user cron expression is required."
        return cronTrigger(cronExpression).build()
    }

    @Provides
    @Singleton
    @Named("autoUnlockUserJobScheduler")
    fun autoUnlockUserJobScheduler(
        @Named("autoUnlockUserJobTrigger") autoUnlockUserJobTrigger: CronTrigger?,
        cmsUserHandler: CmsUserHandler
    ): Scheduler {
        val autoUnlockUserJob = job<AutoUnlockUserJob>(
            "cmsUserHandler" to cmsUserHandler
        ).build()

        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.scheduleJob(autoUnlockUserJob, autoUnlockUserJobTrigger)

        return scheduler
    }
}
