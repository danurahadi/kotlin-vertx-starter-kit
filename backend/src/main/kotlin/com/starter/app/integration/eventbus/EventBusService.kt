package com.starter.app.integration.eventbus

import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.eventbus.deliveryOptionsOf
import kotlinx.coroutines.*
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
class EventBusService @Inject constructor(
    private val vertx: Vertx,
    @param:Named("disableEventBusService") private val disableEventBusService: Boolean,
    @param:Named("eventBusSendTimeout") private val eventBusSendTimeout: Long
) {
    private val log = logger<EventBusService>()

    private fun publish(
        address: String,
        message: JsonObject,
        headers: Map<String, String>,
        printLog: Boolean = true
    ): Boolean {
        return try {
            val options = deliveryOptionsOf()
            options.sendTimeout = eventBusSendTimeout

            headers.forEach {
                options.addHeader(it.key, it.value)
            }

            val res = vertx.eventBus()
                .publish(address, message, options)

            if (printLog) {
                log.eventBusLog(
                    INFO("Publish event bus messages to client via $address has been succeed"),
                    "res" to res,
//                    "message" to message
                )
            }

            true
        } catch (ex: Exception) {
            if (printLog) {
                log.eventBusLog(
                    ERROR("Publish event bus messages to client via $address has been failed"),
                    "errors" to ex.cause.toString()
                )
            }
            throw DataInconsistentException("Publish event bus messages to client via $address has been failed", ex)
        }
    }

    fun publishMessages(
        addressWithMessages: List<Pair<String, JsonObject>>,
        messageHeaders: Map<String, String> = emptyMap(),
        printLog: Boolean = true
    ): List<Job> {
        if (disableEventBusService) {
            throw ServiceUnavailableException("Event bus service is disabled. Please enable it first")
        }
        return addressWithMessages.map {
            CoroutineScope(Dispatchers.Default).launch {
                publish(it.first, it.second, messageHeaders, printLog)
            }
        }
    }
}
