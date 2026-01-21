package com.starter.app.scheduler.job

import com.starter.app.domain.setting.handler.TempFileHandler
import com.starter.app.scheduler.schedulerLog
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.quartz.getData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.quartz.Job
import org.quartz.JobExecutionContext

/**
 * Implementation Class of Temp File Cron Job
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class TempFileJob: Job {
    private val log = logger<TempFileJob>()

    override fun execute(context: JobExecutionContext) {
        val tempFileHandler = context.getData<TempFileHandler>("tempFileHandler") {
            log.schedulerLog(
                ERROR("$it data is required as job data"),
                "context" to context
            )
        }

        val jobResults = runBlocking {
            launch {
                tempFileHandler.autoCleanUnusedFiles()
            }
        }

        log.schedulerLog(
            INFO("Cron job for clean unused files from storage services has been invoked"),
            "jobResults" to jobResults
        )
    }
}
