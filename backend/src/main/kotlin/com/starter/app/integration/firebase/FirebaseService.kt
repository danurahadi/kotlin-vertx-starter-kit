package com.starter.app.integration.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.starter.app.integration.firebase.plain.FcmNotification
import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.web.extension.client.post
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.FileInputStream
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
@DelicateCoroutinesApi
class FirebaseService @Inject constructor(
    private val webClient: WebClient,
    @param:Named("disableFirebase") private val disableFirebase: Boolean,
    @param:Named("firebaseProjectId") private val firebaseProjectId: String,
    @param:Named("firebaseApiBaseUrl") private val firebaseApiBaseUrl: String,
    @param:Named("firebaseServiceAccountKey") private val firebaseServiceAccountKey: String,
    @param:Named("firebaseWebPushTtl") private val firebaseWebPushTtl: Long,
    @param:Named("firebaseWebPushUrgency") private val firebaseWebPushUrgency: String
) {
    private val log = logger<FirebaseService>()

    private suspend fun send(
        tokenWithData: Pair<String, Map<String, String>>,
        notificationMessage: FcmNotification?
    ) {
        if (disableFirebase) {
            throw DataInconsistentException("Firebase service is disabled. Please enable it first.")
        }

        return try {
            val scopes = "https://www.googleapis.com/auth/firebase.messaging"
            val googleCredentials = awaitResult<GoogleCredentials> {
                GoogleCredentials
                    .fromStream(FileInputStream(firebaseServiceAccountKey))
                    .createScoped(listOf(scopes))
            }

            awaitResult<Void> { googleCredentials.refreshIfExpired() }
            val accessToken = googleCredentials.accessToken

            val fcmFullPath = "$firebaseApiBaseUrl/projects/$firebaseProjectId/messages:send"
            val header = mapOf(
                "Content-Type" to "application/json; UTF-8",
                "Authorization" to "Bearer $accessToken"
            )

            val requestBody = if (notificationMessage != null) {
                json {
                    obj(
                        "message" to obj(
                            "token" to tokenWithData.first,
                            "webpush" to obj(
                                "headers" to obj(
                                    "TTL" to firebaseWebPushTtl.toString(),
                                    "Urgency" to firebaseWebPushUrgency
                                ),
                                "data" to tokenWithData.second,
                                "notification" to notificationMessage
                            )
                        )
                    )
                }
            } else {
                json {
                    obj(
                        "message" to obj(
                            "token" to tokenWithData.first,
                            "webpush" to obj(
                                "headers" to obj(
                                    "TTL" to firebaseWebPushTtl.toString(),
                                    "Urgency" to firebaseWebPushUrgency
                                ),
                                "data" to tokenWithData.second
                            )
                        )
                    )
                }
            }
            val resp = webClient.post(absoluteURI = fcmFullPath, body = requestBody, header = header)
            log.firebaseLog(
                INFO("Send messages to FCM HTTP API has been succeed"),
                "response" to resp.bodyAsJsonObject()
            )
        } catch (ex: Exception) {
            val message = ex.message ?: ""
            log.firebaseLog(
                ERROR("Send messages to FCM HTTP API has been failed"),
                "errors" to message
            )
            throw DataInconsistentException(
                "Send messages to FCM HTTP API has been failed. Please try again later.", ex
            )
        }
    }

    fun sendMessages(
        tokensWithDatas: List<Pair<String, Map<String, String>>>,
        notificationMessage: FcmNotification?
    ): List<Job> {
        if (disableFirebase) {
            throw ServiceUnavailableException("Firebase service is disabled. Please enable it first.")
        }
        return tokensWithDatas.map {
            CoroutineScope(Dispatchers.Default).launch {
                send(it, notificationMessage)
            }
        }
    }
}
