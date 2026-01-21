package com.starter.app.scheduler.job

import com.starter.app.domain.user.handler.CmsUserHandler
import com.starter.app.scheduler.schedulerLog
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.quartz.getData
import kotlinx.coroutines.*
import org.quartz.Job
import org.quartz.JobExecutionContext

/**
 * Implementation Class of Auto Unlock User Cron Job
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AutoUnlockUserJob: Job {
    private val log = logger<TempFileJob>()

    override fun execute(context: JobExecutionContext) {
        val cmsUserHandler = context.getData<CmsUserHandler>("cmsUserHandler") {
            log.schedulerLog(
                ERROR("$it data is required as job data"),
                "context" to context
            )
        }

        CoroutineScope(Dispatchers.Default).launch {
            cmsUserHandler.autoUnlockUsers()
        }

        log.schedulerLog(
            INFO("Cron job for auto unlock users has been invoked"),
            "jobResults" to true
        )
    }
}
