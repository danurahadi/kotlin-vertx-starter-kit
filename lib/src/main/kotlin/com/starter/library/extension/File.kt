package com.starter.library.extension

import id.yoframework.web.exception.ValidationException
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

typealias FileName = String
typealias MediaLink = String

fun ByteArray.writeByteArraysToFile(fileName: String): File {

    val file = File(fileName)
    val writer = BufferedOutputStream(FileOutputStream(file))

    writer.write(this)
    writer.close()

    return file
}

fun String.base64StringToFile(fileName: String, maxFileSize: Int? = null): File {
    val decodedBytes = Base64.getDecoder().decode(this)
    val targetFile = "/tmp/${fileName.sanitizeFileName()}"

    val file = decodedBytes.writeByteArraysToFile(targetFile)
    val maxFileSizeInBytes = maxFileSize?.let { it * 1000000 }

    if (maxFileSizeInBytes != null && file.length() > maxFileSizeInBytes) {
        throw ValidationException(listOf("Maximum file size that allowed to upload is $maxFileSize MB."))
    }

    return file
}

fun FileName.parseFileName(type: String): String {
    return when (this.split(".").last()) {
        "jpg" -> "image/jpeg"
        "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        else -> throw ValidationException(listOf("Only JPG, PNG, PDF, DOC, DOCX, XLS, XLSX, and CSV that supported for upload $type."))
    }
}

fun FileName.parseImageFileName(type: String): String {
    return when (this.split(".").last()) {
        "jpg" -> "image/jpeg"
        "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "bmp" -> "image/bmp"
        "gif" -> "image/gif"
        "svg" -> "image/svg+xml"
        "webp" -> "image/webp"
        else -> throw ValidationException(listOf("Only JPG, PNG, GIF, BMP, and SVG that supported for upload $type."))
    }
}

fun MediaLink.parseChatAttachmentLink(): String {
    val splittedAttachment = this.split("/messages/")
    val attachmentFileName = splittedAttachment.last()

    val splittedFileName = attachmentFileName.split(".")
    return when (splittedFileName.last()) {
        "jpg" -> "Photo"
        "jpeg" -> "Photo"
        "png" -> "Photo"
        "doc" -> "Document"
        "docx" -> "Document"
        "pdf" -> "Document"
        else -> "Document"
    }
}
