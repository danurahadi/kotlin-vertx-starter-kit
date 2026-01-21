package com.starter.app.integration.mailer

import com.starter.app.integration.mailer.plain.MailerData
import com.sun.jersey.multipart.FormDataMultiPart
import com.sun.jersey.multipart.file.FileDataBodyPart
import dagger.Module
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.Form
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientResponse
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider
import java.io.File
import javax.inject.Inject

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class Destination(val to: String, val params: Map<String, String> = emptyMap())

@Module
@DelicateCoroutinesApi
class MailerService @Inject constructor(
    private val sender: String,
    private val mailerUrl: String,
    private val account: Pair<String, String>,
    private val disabled: Boolean
) {
    private val log = logger(MailerService::class)

    private fun credentials(): Pair<String, HttpAuthenticationFeature> {
        return mailerUrl to
                HttpAuthenticationFeature.basic(account.first, account.second)
    }

    fun sendMessage(
        to: String,
        subject: String,
        html: String,
        attachments: List<File> = emptyList()
    ): JsonObject {
        val destinations = listOf(Destination(to))
        return sendMessage(destinations, subject, html, attachments)
    }

    suspend fun sendBulkMessage(
        destinations: List<Destination>,
        subject: String,
        html: String,
        attachments: List<File> = emptyList()
    ): List<JsonObject> {
        return destinations.partition(500).map {
            CoroutineScope(Dispatchers.Default).async {
                sendMessage(it, subject, html, attachments)
            }
        }.awaitAll()
    }

    suspend fun sendBulkMessages(
        mailerData: List<MailerData>
    ): List<JsonObject> {
        return mailerData.map {
            CoroutineScope(Dispatchers.Default).async {
                sendMessage(
                    listOf(Destination(to = it.recipient ?: "")),
                    it.subject,
                    it.htmlContent,
                    it.attachments
                )
            }
        }.awaitAll()
    }

    private fun sendMessage(
        destinations: List<Destination>,
        subject: String,
        html: String,
        attachments: List<File>
    ): JsonObject {

        val credentials = credentials()
        val clientConfig = ClientConfig()

        clientConfig.register(credentials.second)
        clientConfig.register(JacksonJsonProvider::class.java)

        val client = ClientBuilder.newClient(clientConfig)
        val webTarget = client.target(credentials.first)

        fun post(): ClientResponse? {
            return if (attachments.isEmpty()) {
                val formData = Form()

                formData.param("from", sender)
                destinations.forEach {
                    formData.param("to", it.to)
                }

                formData.param("recipient-variables", destinations.toJson().toString())
                formData.param("subject", subject)
                formData.param("html", html)

                webTarget
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(
                        formData, MediaType.APPLICATION_FORM_URLENCODED_TYPE
                    ), ClientResponse::class.java)
            } else {
                val formDataMultiPart = FormDataMultiPart()

                formDataMultiPart.field("from", sender)
                destinations.forEach {
                    formDataMultiPart.field("to", it.to)
                }

                formDataMultiPart.field("recipient-variables", destinations.toJson().toString())
                formDataMultiPart.field("subject", subject)
                formDataMultiPart.field("html", html)

                attachments.filter { it.isFile && it.exists() }.forEach { file ->
                    val filePart = FileDataBodyPart("file", file)
                    formDataMultiPart.field("file", "attachment")
                        .bodyPart(filePart)
                }

                webTarget
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(
                        formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE
                    ), ClientResponse::class.java)
            }
        }

        return try {

            if (disabled) throw IllegalAccessError("Mailer is disabled")

            val (clientResponse, time) = executeTimeMillis { post() }
            if (clientResponse == null) {
                throw IllegalStateException("Response body from mail service provider is null")
            }

            val statusCode = clientResponse.statusInfo?.statusCode
            if (statusCode != 200) {
                throw IllegalStateException("Response status code from mail service provider is not OK")
            }

            val body = JsonObject(clientResponse.entity.toString())
            val resultJson = JsonObject()

            resultJson.put("status", clientResponse.statusInfo?.statusCode)
            resultJson.put("reason", clientResponse.statusInfo?.reasonPhrase)

            resultJson.put("class", clientResponse.statusInfo?.family?.name)
            resultJson.put("body", body)

            log.mailerLog(
                INFO("Success Send Email"),
                "TIME_TAKEN" to time,
                "MAILER_URL" to mailerUrl,
                "SENDER" to sender,
                "SUBJECT" to subject,
//                "HTML" to html,
                "DESTINATION" to destinations,
                "RESPONSE_BODY" to body,
                "RESPONSE_CODE" to "${clientResponse.status}"
            )
            resultJson

        } catch (throwable: Exception) {

            log.mailerLog(
                ERROR("Exception when Send Email", throwable),
                "MAILER_URL" to mailerUrl,
                "SENDER" to sender,
                "SUBJECT" to subject,
//                "HTML" to html,
                "DESTINATION" to destinations
            )

            val resultJson = JsonObject()

            resultJson.put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.toString())
            resultJson.put("reason", throwable.message)

            resultJson

        }
    }
}

private fun List<Destination>.partition(partitionSize: Int): List<List<Destination>> {
    val maxIteration = this.size / partitionSize
    return (0..maxIteration).map {
        val start = it * partitionSize
        val end = start + partitionSize
        this.subList(start, if (end >= this.size) this.size else end)
    }.filter { it.isNotEmpty() }
}

private fun List<Destination>.toJson(): JsonObject {
    return this.associate { it.to to it.params.toJson() }
        .toJson()
}

private fun Map<String, Any?>.toJson(): JsonObject {
    return this.toList().fold(JsonObject()) { json, (key, value) ->
        json.put(key, value)
    }
}

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
suspend fun MailerService.sendEmail(
    to: String,
    subject: String,
    html: String,
    attachments: List<File> = emptyList()
): JsonObject {
    return this.sendMessage(to, subject, html, attachments)
}

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
suspend fun MailerService.sendBulkEmail(
    to: List<Destination>,
    subject: String,
    html: String,
    attachments: List<File> = emptyList()
): List<JsonObject> {
    return this.sendBulkMessage(to, subject, html, attachments)
}
