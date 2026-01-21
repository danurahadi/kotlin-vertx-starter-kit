package com.starter.app.integration.slack

import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.web.extension.client.post
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
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
class SlackService @Inject constructor(
    private val webClient: WebClient,
    @param:Named("disableSlackApi") private val disableSlackApi: Boolean,
    @param:Named("slackWebhookUrl") private val slackWebhookUrl: String
) {
    private val log = logger<SlackService>()
    private val slackThreadPool = newFixedThreadPoolContext(nThreads = 4, name = "Slack Service Thread Pool")

    suspend fun postMessage(message: String): Job {
        return coroutineScope {
            launch {
                if (!disableSlackApi) {

                    val header = mapOf(
                        "Content-Type" to "application/json"
                    )

                    val body = json {
                        obj(
                            "text" to message
                        )
                    }

                    val resp = withContext(slackThreadPool) {
                        webClient.post(
                            absoluteURI = slackWebhookUrl,
                            body = body,
                            header = header
                        ).bodyAsString()
                    }

                    log.slackLog(
                        INFO("Post message to Slack channel for error reporting has been succeed"),
                        "resp" to resp
                    )
                }
            }
        }
    }
}
