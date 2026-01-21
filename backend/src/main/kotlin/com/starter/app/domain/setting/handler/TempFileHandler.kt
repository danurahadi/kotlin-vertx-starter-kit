package com.starter.app.domain.setting.handler

import com.starter.app.domain.setting.db.repository.TempFileRepository
import com.starter.app.domain.setting.settingLog
import com.starter.app.integration.fileupload.aws.AwsS3Service
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class TempFileHandler @Inject constructor(
    private val tempFileRepository: TempFileRepository,
    private val awsS3Service: AwsS3Service
) {
    private val log = logger<TempFileHandler>()

    suspend fun autoCleanUnusedFiles(): Boolean {
        /**
         * Core process
         *
         */
        val (cleaned, coreTime) = executeTimeMillis {
            try {
                val expiredFiles = tempFileRepository.findExpiredFiles().findList()
                val expiredFileMediaLinks = expiredFiles.map { it.mediaLink }

                if (expiredFileMediaLinks.isNotEmpty()) {
                    awsS3Service.deleteFiles(expiredFileMediaLinks)
                    tempFileRepository.deleteAll(list = expiredFiles)

                    true
                } else {
                    false
                }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Auto clean unused files from Storage services has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Auto clean unused files from Storage services has been succeed"),
            "coreTime" to coreTime,
            "cleaned" to cleaned
        )
        /**
         * End of core process
         *
         */

        return true
    }
}
