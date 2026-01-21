package com.starter.app.integration.fileupload.aws

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.DeleteObjectsRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.starter.app.integration.fileupload.aws.plain.AwsS3Data
import com.starter.app.integration.fileupload.aws.plain.AwsS3Migration
import com.starter.library.extension.parseFileName
import com.starter.library.extension.parseImageFileName
import com.starter.library.extension.sanitizeFileName
import com.starter.library.extension.writeByteArraysToFile
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.extra.snowflake.nextAlpha
import id.yoframework.web.exception.ValidationException
import io.vertx.core.json.Json
import kotlinx.coroutines.*
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.naming.ServiceUnavailableException

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AwsS3Service @Inject constructor(
    @param:Named("disableAwsS3") private val disableAwsS3: Boolean,
    @param:Named("awsAccessKey") private val awsAccessKey: String,
    @param:Named("awsSecretKey") private val awsSecretKey: String,
    @param:Named("awsBaseUrl") private val awsBaseUrl: String,
    @param:Named("awsAliasUrl") private val awsAliasUrl: String,
    @param:Named("awsRegionName") private val awsRegionName: String,
    @param:Named("awsBucketName") private val awsBucketName: String,
    @param:Named("maxFileSize") private val maxFileSize: Int,
    @param:Named("maxImageFileSize") private val maxImageFileSize: Int
) {
    private val log = logger<AwsS3Service>()
    private val awsS3ServicePool = newFixedThreadPoolContext(nThreads = 8, name = "AWS S3 Service Pool")

    private fun uploadFile(
        folderName: String,
        fileName: String,
        file: File
    ): AwsS3Data {

        val cacheControl = "public, max-age=31536000"
        val contentType = fileName.parseFileName("file")

        try {

            // setup AWS S3 Credentials and Endpoint
            val awsS3credentials = AWSStaticCredentialsProvider(BasicAWSCredentials(awsAccessKey, awsSecretKey))
            val endpoint = "https://$awsRegionName.$awsBaseUrl"

            // setup AWS S3 Client
            val awsS3client = AmazonS3ClientBuilder.standard()
                .withCredentials(awsS3credentials)
                .withEndpointConfiguration(EndpointConfiguration(endpoint, awsRegionName))
                .build()

            // setup Object Metadata
            val metadata = ObjectMetadata().apply {
                setContentType(contentType)
                setCacheControl(cacheControl)
            }

            // Setup Snowflake ID to the fileName
            val fileExt = fileName.split(".").last()
            val fileNameWithoutExt = fileName.replaceAfterLast(".", "")
                .replace(".", "-")

            val filePath = "$folderName/$fileNameWithoutExt${nextAlpha(30)}.$fileExt"

            // Upload file to AWS S3 / DO Spaces
            val putObjectReq = PutObjectRequest(awsBucketName, filePath, file)
                .withCannedAcl(CannedAccessControlList.PublicRead)
                .withMetadata(metadata)

            val resp = awsS3client.putObject(putObjectReq)
            val mediaLink = "$awsAliasUrl/$filePath"

            // delete the temp file
            file.deleteOnExit()

            log.awsS3Log(
                INFO("Upload file $filePath to AWS S3 / DO Spaces has been succeed"),
                "RESPONSE" to resp.toString(),
                "PUBLIC_LINK" to mediaLink,
                "CONTENT_LENGTH" to file.readBytes().size,
                "CONTENT_TYPE" to contentType
            )

            return AwsS3Data(
                originalFileName = filePath,
                thumbnailFileName = null,
                originalLink = mediaLink,
                thumbnailLink = null,
                resizeFailed = false
            )
        } catch (ase: AmazonServiceException) {
            val message = ase.message ?: ""
            log.awsS3Log(
                ERROR("Exception (ASE) when upload image file $fileName to AWS S3 / DO Spaces"),
                "ERROR_CODE" to ase.errorCode,
                "MESSAGE" to message
            )
            throw ase
        } catch (ace: AmazonClientException) {
            val message = ace.message ?: ""
            log.awsS3Log(
                ERROR("Exception (ACE) when upload image file $fileName to AWS S3 / DO Spaces"),
                "MESSAGE" to message,
                "CONTENT_TYPE" to contentType
            )
            throw ace
        }
    }

    fun uploadImage(
        folderName: String,
        fileTitle: String,
        file: File,
        thumbnailSize: Int?,
        originalSize: Int?,
        square: Boolean,
        thumbnailSquare: Boolean,
        convertToWebp: Boolean = true
    ): AwsS3Data {

        val fileName = if (convertToWebp) {
            val fileNameWithoutExt = fileTitle.replaceAfter(".", "")
                .replace(".", "")
            "$fileNameWithoutExt.webp"
        } else fileTitle

        val cacheControl = "public, max-age=31536000"
        val contentType = fileName.parseImageFileName("image")

        val webpFile = if (convertToWebp) {
            val fileNameWithoutExt = fileName.replaceAfter(".", "")
                .replace(".", "")

            val webpFileName = "$fileNameWithoutExt.webp"
            val webpFilePath = "/tmp/$webpFileName"

            val bufferedImage = ImageIO.read(file)
            val webpFile = File(webpFilePath)

            ImageIO.write(bufferedImage, "webp", webpFile)
            webpFile
        } else file

        // setup AWS S3 Credentials and Endpoint
        val awsS3credentials = AWSStaticCredentialsProvider(BasicAWSCredentials(awsAccessKey, awsSecretKey))
        val endpoint = "https://$awsRegionName.$awsBaseUrl"

        // setup AWS S3 Client
        val awsS3client = AmazonS3ClientBuilder.standard()
            .withCredentials(awsS3credentials)
            .withEndpointConfiguration(EndpointConfiguration(endpoint, awsRegionName))
            .build()

        try {
            // setup Object Metadata for original file
            val metadata = ObjectMetadata().apply {
                setContentType(contentType)
                setCacheControl(cacheControl)
            }

            // Setup Snowflake ID to the fileName
//            val fileExt = fileName.split(".").last()
//            val fileNameWithoutExt = fileName.replaceAfterLast(".", "")
//                .replace(".", "-")

            val originalFilePath = "$folderName/originals/$fileName"
            val imageFile = try {
                ImageIO.read(webpFile)
            } catch (_: Exception) {
                null
            }

            // Upload original file to AWS S3 / DO Spaces
            val resizeFailed = if (originalSize != null && imageFile != null && imageFile.width > originalSize) {
                try {
                    if (square) {
                        Thumbnails.of(webpFile)
                            .forceSize(originalSize, originalSize)
                            .outputQuality(1.0)
                            .toFile(webpFile)
                    } else {
                        Thumbnails.of(webpFile)
                            .width(originalSize)
                            .outputQuality(1.0)
                            .toFile(webpFile)
                    }
                    false
                } catch (_: Exception) {
                    true
                }
            } else {
                true
            }

            val putObjectOriginalReq = PutObjectRequest(awsBucketName, originalFilePath, webpFile)
                .withCannedAcl(CannedAccessControlList.PublicRead)
                .withMetadata(metadata)

            val originalResp = awsS3client.putObject(putObjectOriginalReq)
            val originalLink = "$awsAliasUrl/$originalFilePath"

            log.awsS3Log(
                INFO("Upload original image file $originalFilePath to AWS S3 / DO Spaces has been succeed"),
                "RESPONSE" to originalResp.toString(),
                "PUBLIC_LINK" to originalLink,
                "CONTENT_LENGTH" to webpFile.readBytes().size,
                "CONTENT_TYPE" to contentType
            )

            // Upload thumbnail file to AWS S3 / DO Spaces
            val thumbnailObject = if (thumbnailSize != null) {

                // setup Object Metadata for thumbnail file
                val thumbnailMetadata = ObjectMetadata().apply {
                    setContentType(contentType)
                    setCacheControl(cacheControl)
                }

                try {
                    if (thumbnailSquare) {
                        Thumbnails.of(webpFile)
                            .forceSize(thumbnailSize, thumbnailSize)
                            .outputQuality(1.0)
                            .toFile(webpFile)
                    } else {
                        Thumbnails.of(webpFile)
                            .width(thumbnailSize)
                            .outputQuality(1.0)
                            .toFile(webpFile)
                    }
                } catch (_: Exception) {
                    // continue to upload
                }

                val thumbnailFilePath = "$folderName/thumbnails/$fileName"
                val putObjectThumbnailReq = PutObjectRequest(awsBucketName, thumbnailFilePath, webpFile)
                    .withCannedAcl(CannedAccessControlList.PublicRead)
                    .withMetadata(thumbnailMetadata)

                val thumbnailResp = awsS3client.putObject(putObjectThumbnailReq)
                val thumbnailLink = "$awsAliasUrl/$thumbnailFilePath"

                log.awsS3Log(
                    INFO("Upload thumbnail image file $thumbnailFilePath " +
                            "to AWS S3 / DO Spaces has been succeed"),
                    "RESPONSE" to thumbnailResp.toString(),
                    "PUBLIC_LINK" to thumbnailLink,
                    "CONTENT_LENGTH" to webpFile.readBytes().size,
                    "CONTENT_TYPE" to contentType
                )

                thumbnailFilePath to thumbnailLink
            } else {
                null
            }

            // delete the temp file
            webpFile.deleteOnExit()

            return AwsS3Data(
                originalFileName = originalFilePath,
                thumbnailFileName = thumbnailObject?.first,
                originalLink = originalLink,
                thumbnailLink = thumbnailObject?.second,
                resizeFailed = resizeFailed
            )
        } catch (ase: AmazonServiceException) {
            val message = ase.message ?: ""
            log.awsS3Log(
                ERROR("Exception (ASE) when upload image file $fileName to AWS S3 / DO Spaces"),
                "ERROR_CODE" to ase.errorCode,
                "MESSAGE" to message
            )
            throw ase
        } catch (ace: AmazonClientException) {
            val message = ace.message ?: ""
            log.awsS3Log(
                ERROR("Exception (ACE) when upload image file $fileName to AWS S3 / DO Spaces"),
                "MESSAGE" to message,
                "CONTENT_TYPE" to contentType
            )
            throw ace
        }
    }

    private fun downloadFile(dataId: Long?, objectKey: String): AwsS3Migration {

        try {
            // setup AWS S3 Credentials and Endpoint
            val awsS3credentials = AWSStaticCredentialsProvider(BasicAWSCredentials(awsAccessKey, awsSecretKey))
            val endpoint = "https://$awsRegionName.$awsBaseUrl"

            // setup AWS S3 Client
            val awsS3client = AmazonS3ClientBuilder.standard()
                .withCredentials(awsS3credentials)
                .withEndpointConfiguration(EndpointConfiguration(endpoint, awsRegionName))
                .build()

            // get AWS S3 Object
            val awsS3Object = awsS3client.getObject(awsBucketName, objectKey)
            val inputStream = awsS3Object.objectContent
            val contentType = awsS3Object.objectMetadata.contentType

            // create new temp file
            val tmpFilePath = "/tmp/$objectKey"
            val tmpFile = File(tmpFilePath)

            // write input stream to the created new temp file
            FileUtils.copyInputStreamToFile(inputStream, tmpFile)

            // close the input stream
            inputStream.close()

            // setup folder name & new file name (without Snowflake ID)
            val splittedObjectKey = objectKey.split("/")
            val folderName = splittedObjectKey[0]
            val fileName = splittedObjectKey[1]

            val fileExt = fileName.split(".").last()
            val fileNameWithoutExt = fileName.replaceAfterLast("-", "").dropLast(1)
            val newFileName = "$fileNameWithoutExt.$fileExt"

            log.awsS3Log(
                INFO("Download file $objectKey from AWS S3 / DO Spaces has been succeed"),
                "tmpFile" to tmpFilePath
            )

            return AwsS3Migration(
                dataId = dataId,
                filePath = tmpFilePath,
                fileName = newFileName,
                folderName = folderName,
                contentType = contentType
            )
        } catch (ase: AmazonServiceException) {
            val message = ase.message ?: ""
            log.awsS3Log(
                ERROR("Exception (ASE) when download file $objectKey from AWS S3 / DO Spaces"),
                "ERROR_CODE" to ase.errorCode,
                "MESSAGE" to message,
                "DATA_ID" to dataId.toString()
            )
            throw ase
        } catch (ace: AmazonClientException) {
            val message = ace.message ?: ""
            log.awsS3Log(
                ERROR("Exception (ACE) when download file $objectKey from AWS S3 / DO Spaces"),
                "MESSAGE" to message,
                "DATA_ID" to dataId.toString()
            )
            throw ace
        } catch (ex: Exception) {
            val message = ex.message ?: ""
            log.awsS3Log(
                ERROR("Exception (Kotlin) when download file $objectKey from AWS S3 / DO Spaces"),
                "MESSAGE" to message,
                "DATA_ID" to dataId.toString()
            )
            throw ex
        }
    }

    private fun delete(mediaLinks: List<String>): Boolean {

        return try {
            // setup AWS S3 Credentials and Endpoint
            val awsS3credentials = AWSStaticCredentialsProvider(BasicAWSCredentials(awsAccessKey, awsSecretKey))
            val endpoint = "https://$awsRegionName.$awsBaseUrl"

            // setup AWS S3 Client
            val awsS3client = AmazonS3ClientBuilder.standard()
                .withCredentials(awsS3credentials)
                .withEndpointConfiguration(EndpointConfiguration(endpoint, awsRegionName))
                .build()

            // setup Object key list
            val keyNames = mediaLinks.map {
                it.split("$awsAliasUrl/").last()
            }

            // setup Delete Objects Request
            val deleteObjectsRequest = DeleteObjectsRequest(awsBucketName)
                .withKeys(*keyNames.toTypedArray())

            // delete Objects from AWS S3 / DO Spaces
            val resp = awsS3client.deleteObjects(deleteObjectsRequest)

            log.awsS3Log(
                INFO("Delete files ${keyNames.joinToString(", ")} " +
                        "from AWS S3 / DO Spaces has been succeed"),
                "resp" to resp.toString()
            )

            true
        } catch (e: Exception) {
            log.awsS3Log(
                ERROR("Exception when delete files ${ mediaLinks.joinToString(", ") } " +
                        "from AWS S3 / DO Spaces"),
                "EX_MESSAGE" to Json.encode(e)
            )
            false
        }
    }

    suspend fun uploadFileBase64(
        fileName: String,
        folderName: String,
        base64String: String
    ): AwsS3Data {
        if (disableAwsS3) throw ServiceUnavailableException("AWS S3 service is disabled. Please enable it first")
        
        val decodedBytes = Base64.getDecoder().decode(base64String)
        val targetFile = "/tmp/${fileName.sanitizeFileName()}"

        val file = decodedBytes.writeByteArraysToFile(targetFile)
        val maxFileSizeInBytes = maxFileSize * 1000000

        if (file.length() > maxFileSizeInBytes) {
            throw ValidationException(listOf("Maximum file size that allowed to upload is $maxFileSize MB."))
        }
        return uploadFile(folderName, file.name, file)
    }

    suspend fun uploadImageFileBase64(
        fileName: String,
        folderName: String,
        base64String: String,
        thumbnailSize: Int?,
        originalSize: Int?,
        square: Boolean,
        thumbnailSquare: Boolean = true,
        convertToWebp: Boolean = true
    ): AwsS3Data {
        if (disableAwsS3) throw ServiceUnavailableException("AWS S3 service is disabled. Please enable it first")

        val decodedBytes = Base64.getDecoder().decode(base64String)
        val targetFile = "/tmp/${fileName.sanitizeFileName()}"

        val file = decodedBytes.writeByteArraysToFile(targetFile)
        val maxFileSizeInBytes = maxImageFileSize * 1000000

        if (file.length() > maxFileSizeInBytes) {
            throw ValidationException(listOf("Maximum image file size that allowed to upload is $maxImageFileSize MB."))
        }
        return uploadImage(
            folderName, file.name, file, thumbnailSize, originalSize, square, thumbnailSquare, convertToWebp
        )
    }

    suspend fun uploadImageFilesForMigration(
        awsS3Migrations: List<AwsS3Migration?>,
        thumbnailSize: Int?,
        originalSize: Int?,
        square: Boolean,
        thumbnailSquare: Boolean = true,
        convertToWebp: Boolean = true
    ): List<Pair<DataId?, AwsS3Data?>> {
        if (disableAwsS3) throw ServiceUnavailableException("AWS S3 service is disabled. Please enable it first")

        return awsS3Migrations.map { sm ->
            coroutineScope {
                async(awsS3ServicePool) {
                    if (sm != null) {
                        val file = File(sm.filePath)
                        val dataId = sm.dataId

                        val awsS3Data = uploadImage(
                            sm.folderName,
                            sm.fileName,
                            file,
                            thumbnailSize,
                            originalSize,
                            square,
                            thumbnailSquare,
                            convertToWebp
                        )
                        dataId to awsS3Data
                    } else {
                        null to null
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun downloadFiles(dataIdsWithMediaLinks: List<Pair<DataId?, AwsS3MediaLink?>>): List<AwsS3Migration?> {
        if (disableAwsS3) throw ServiceUnavailableException("AWS S3 service is disabled. Please enable it first")

        return dataIdsWithMediaLinks.map { l ->
            coroutineScope {
                async(awsS3ServicePool) {
                    val dataId = l.first
                    val mediaLink = l.second

                    if (mediaLink != null) {
                        val objectKey = mediaLink.split(
                            "https://$awsBucketName.$awsRegionName.$awsBaseUrl/"
                        ).last()
                        downloadFile(dataId, objectKey)
                    } else {
                        null
                    }
                }
            }
        }.awaitAll()
    }

    suspend fun deleteFiles(mediaLinks: List<String>): Boolean {
        if (disableAwsS3) throw ServiceUnavailableException("AWS S3 service is disabled. Please enable it first")
        return delete(mediaLinks)
    }
}
