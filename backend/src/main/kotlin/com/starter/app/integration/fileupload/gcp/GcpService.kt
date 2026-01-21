package com.starter.app.integration.fileupload.gcp

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.storage.Acl
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import id.yoframework.core.extension.filesystem.readFileAsync
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.extra.snowflake.nextId
import io.vertx.core.buffer.Buffer
import io.vertx.core.file.FileSystem
import io.vertx.core.json.Json
import io.vertx.ext.web.FileUpload
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.newFixedThreadPoolContext
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.naming.ServiceUnavailableException

/**
 * Service for integrating Google Cloud Platform storage service with Starter API v1.0
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class GcpService @Inject constructor(
    private val fileSystem: FileSystem,
    @param:Named("gcpCredentialsPath") private val gcpCredentialsPath: String,
    @param:Named("disableGcp") private val disableGcp: Boolean
 ) {

    private val log = logger<GcpService>()
    private val fileUploadPool = newFixedThreadPoolContext(nThreads = 4, name = "FileUpload Pool")

    private fun post(
        bucketName: String,
        fileName: String,
        file: Buffer,
        contentType: String
    ): Pair<GcpFileName, GcpMediaLink> {
//      val storage = StorageOptions.getDefaultInstance().service
        val storage = StorageOptions.newBuilder()
            .setCredentials(ServiceAccountCredentials.fromStream(FileInputStream(gcpCredentialsPath)))
            .build()
            .service

        try {
            // Modify access list to allow all users with link to read file
            val acls = ArrayList<Acl>()
            acls.add(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))

            // Setup Snowflake ID to the fileName
            val fileExt = fileName.split(".").last()
            val fileNameWithoutExt = fileName.replaceAfterLast(".", "")
                .replace(".", "-")
            val uniqueFileName = "$fileNameWithoutExt${nextId(30)}.$fileExt"

            // The input stream is closed by default, so we don't need to close it here
            val blob = storage.create(
                BlobInfo.newBuilder(bucketName, uniqueFileName).setAcl(acls).setContentType(contentType).build(),
                file.bytes
            )
            val publicLink = "https://storage.googleapis.com/${blob.bucket}/${blob.name}"

            log.gcpLog(
                INFO("Upload file $uniqueFileName to google cloud storage has been succeed"),
                "BLOB_NAME" to blob.name,
                "PUBLIC_LINK" to publicLink,
                "CONTENT_LENGTH" to file.bytes.size
            )

            return (blob.name to publicLink)
        } catch (e: Exception) {
            log.gcpLog(
                ERROR("Exception when upload file $fileName to google cloud storage"),
                "FILE_NAME" to fileName,
                "CONTENT_LENGTH" to file.bytes.size,
                "EX_MESSAGE" to Json.encode(e)
            )

            throw e
        }
    }

    suspend fun post(bucketName: String, fileUpload: Pair<FileUpload, String>): Pair<GcpFileName, GcpMediaLink> {
        val uploadedFile = fileSystem.readFileAsync(fileUpload.first.uploadedFileName())
        val fileName = fileUpload.first.fileName().replace(" ", "-")

        val contentType = fileUpload.second
        return post(bucketName, fileName, uploadedFile, contentType)
    }

    suspend fun uploadFiles(
        bucketName: String,
        fileUploadList: List<Pair<FileUpload, String>>
    ): List<Pair<GcpFileName, GcpMediaLink>> {
        if (disableGcp) throw ServiceUnavailableException("GCP service is disabled. Please enable it first")

        return fileUploadList.map {
            coroutineScope {
                async(fileUploadPool) { post(bucketName, it) }
            }
        }.map { it.await() }
    }

    suspend fun uploadFile(
        bucketName: String,
        fileUpload: Pair<FileUpload, String>
    ): Pair<GcpFileName, GcpMediaLink> {
        if (disableGcp) throw ServiceUnavailableException("GCP service is disabled. Please enable it first")
        return post(bucketName, fileUpload)
    }

    private fun delete(bucketName: String, fileName: String): String {
        val storage = StorageOptions.newBuilder()
            .setCredentials(ServiceAccountCredentials.fromStream(FileInputStream(gcpCredentialsPath)))
            .build()
            .service

        try {
            val deleted = storage.delete(
                BlobId.of(bucketName, fileName)
            )

            if (deleted) {
                log.gcpLog(
                    INFO("Delete file $fileName from google cloud storage has been succeed"),
                    "BUCKET_NAME" to bucketName,
                    "FILE_NAME" to fileName
                )
            } else {
                log.gcpLog(
                    INFO("Delete file $fileName from google cloud storage has been succeed, " +
                            "but the blob was not found"),
                    "BUCKET_NAME" to bucketName,
                    "FILE_NAME" to fileName
                )
            }

            return fileName
        } catch (e: Exception) {
            log.gcpLog(
                ERROR("Exception when delete file $fileName from google cloud storage"),
                "FILE_NAME" to fileName,
                "EX_MESSAGE" to Json.encode(e)
            )

            throw e
        }
    }

    suspend fun deleteFiles(bucketName: String, fileNames: List<String>): List<String> {
        if (disableGcp) throw ServiceUnavailableException("GCP service is disabled. Please enable it first")
        return fileNames.map {
            coroutineScope {
                async(fileUploadPool) { delete(bucketName, it) }
            }
        }.map { it.await() }
    }

    fun deleteFile(bucketName: String, fileName: String): String {
        if (disableGcp) throw ServiceUnavailableException("GCP service is disabled. Please enable it first")
        return delete(bucketName, fileName)
    }
}
