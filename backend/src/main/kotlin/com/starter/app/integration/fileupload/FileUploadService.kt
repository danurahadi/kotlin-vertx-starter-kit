package com.starter.app.integration.fileupload

import com.starter.library.extension.FileName
import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.extension.filesystem.readFileAsync
import id.yoframework.core.extension.filesystem.writeFileAsync
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.web.extension.client.get
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import io.vertx.ext.web.FileUpload
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.*
import java.io.File
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
class FileUploadService @Inject constructor(
    private val fileSystem: FileSystem,
    private val webClient: WebClient
) {

    private val fileUploadPool = newFixedThreadPoolContext(nThreads = 4, name = "FileUpload Pool")
    private val log = logger<FileUploadService>()

    private suspend fun write(directory: String, fileName: String, file: Buffer): Pair<FileName, FilePath> {
        val filePath = "$directory/$fileName"
        try {
            val (_, time) = executeTimeMillis {
                fileSystem.writeFileAsync(filePath, file)
            }

            log.fileUploadLog(
                INFO("Uploaded File $filePath"),
                "TIME_TAKEN" to time,
                "FILE_PATH" to filePath,
                "FILE_SIZE" to file.length()
            )

            return fileName to filePath
        } catch (e: Exception) {
            log.fileUploadLog(
                ERROR("Exception when Uploaded File $filePath"),
                "FILE_PATH" to filePath,
                "FILE_SIZE" to file.length()
            )
            throw e
        }
    }

    private suspend fun write(directory: String, fileUpload: FileUpload): Pair<FileName, FilePath> {
        val uploadedFile = fileSystem.readFileAsync(fileUpload.uploadedFileName())
        val fileName = fileUpload.fileName().replace(" ", "-")
        return write(directory, fileName, uploadedFile)
    }

    suspend fun downloadFile(remoteURL: String, directory: String): Pair<File, FileName> {
        val splittedRemoteURL = remoteURL.split("/res/image/data/")
        val fileName = splittedRemoteURL[1]

        val filePath = "$directory/$fileName"
        log.fileUploadLog(
            INFO("Download file... $remoteURL")
        )

        val response = webClient.get(remoteURL, emptyMap())

        log.fileUploadLog(
            INFO("Download completed $remoteURL")
        )
        return if (response.statusCode() == 200) {

            // download successful, save the file locally
            val buffer = response.body()
            try {

                log.fileUploadLog(
                    INFO("Write file to local machine... $filePath")
                )

                val (_, time) = executeTimeMillis {
                    fileSystem.writeFileAsync(filePath, buffer)
                }

                log.fileUploadLog(
                    INFO("File has been downloaded & written to local machine : $filePath"),
                    "TIME_TAKEN" to time,
                    "FILE_PATH" to filePath,
                    "FILE_SIZE" to buffer.length()
                )
                val file = File(filePath)

                // return downloaded file & filename as Pair
                file to fileName
            } catch (e: Exception) {
                log.fileUploadLog(
                    ERROR("Exception when Download File $filePath"),
                    "FILE_PATH" to filePath,
                    "FILE_SIZE" to buffer.length()
                )
                throw e
            }
        } else throw DataInconsistentException("Download file has been failed. Try again.")
    }

    suspend fun writeAll(directory: String, fileUploadList: List<FileUpload>): List<Pair<FileName, FilePath>> {
        return fileUploadList.map {
            coroutineScope {
                async(fileUploadPool) { write(directory, it) }
            }
        }.awaitAll()
    }
}
