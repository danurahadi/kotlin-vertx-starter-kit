package com.starter.app.integration.google

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.starter.app.integration.google.plain.RecaptchaResponse
import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.json.get
import id.yoframework.web.exception.BadRequestException
import id.yoframework.web.exception.orDataError
import id.yoframework.web.extension.client.jsonBody
import id.yoframework.web.extension.client.postForm
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.client.WebClient
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/***
 *
 * Google API integration wrapper functions
 *
 * @author Argi Danu Rahadi
 * @email danu.argi@gmail.com
 *
 ***/

@Singleton
class GoogleService @Inject constructor(
    @param:Named("disableGoogleApi") private val disableGoogleApi: Boolean,
    @param:Named("googleApiClientId") private val googleApiClientId: String,
    @param:Named("googleBaseUrl") private val googleBaseUrl: String,
    @param:Named("googleRecaptchaServerKey") private val googleRecaptchaServerKey: String,
    private val webClient: WebClient
) {
    private val log = logger<GoogleService>()

    private fun verifyToken(googleIDToken: String): GoogleIdToken.Payload {

        val verifier = GoogleIdTokenVerifier.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance()
        )
            .setAudience(listOf(googleApiClientId))
            .build()

        val idToken = verifier.verify(googleIDToken)
        return if (idToken != null) {
            val payload = idToken.payload
            log.googleLog(
                INFO("Verify Google ID Token has been succeed"),
                "payload" to payload
            )
            payload
        } else {
            log.googleLog(
                ERROR("Verify Google ID Token has been failed")
            )
            throw BadRequestException(
                "Invalid Google ID Token. Please try again later."
            )
        }
    }

    fun verifyIdToken(idToken: String): GoogleIdToken.Payload {
        if (disableGoogleApi) {
            throw DataInconsistentException("Google API is disabled. Please enable it first.")
        }
        return verifyToken(idToken)
    }

    suspend fun verifyRecaptchaToken(token: String, ipAddress: String): RecaptchaResponse {
        if (disableGoogleApi) {
            throw DataInconsistentException("Google API is disabled. Please enable it first.")
        }
        val (result, coreTime) = executeTimeMillis {
            try {
                val header = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/json"
                )
                val formData = mapOf(
                    "secret" to googleRecaptchaServerKey,
                    "response" to token,
                    "remoteIp" to ipAddress
                )

                val absoluteURI = "$googleBaseUrl/recaptcha/api/siteverify"
                val response = webClient
                    .postForm(
                        absoluteURI = absoluteURI,
                        header = header,
                        formData = formData
                    ).jsonBody()

                val success = response.get<Boolean>("success") orDataError "Invalid google recaptcha verify response 'success'."
                val challengeTs = response.get<String>("challenge_ts")
                val hostname = response.get<String>("hostname")

                val score = response.get<Double>("score")
                val errorCodes = response.get<JsonArray>("error-codes")

                RecaptchaResponse(
                    success = success,
                    challengeTs = challengeTs,
                    hostname = hostname,
                    score = score,
                    errorCodes = errorCodes?.map { it.toString() },
                )
            } catch (ex: Exception) {
                val errorMessage = ex.message ?: ""
                log.googleLog(
                    ERROR("Google Recaptcha Verify API request has been failed"),
                    "error" to errorMessage
                )
                throw ex
            }
        }
        log.googleLog(
            INFO("Google Recaptcha Verify API request has been succeed"),
            "coreTime" to coreTime,
            "response" to result
        )
        return result
    }
}
