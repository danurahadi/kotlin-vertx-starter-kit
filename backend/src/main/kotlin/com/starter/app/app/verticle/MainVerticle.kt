package com.starter.app.app.verticle

import com.starter.app.app.controller.MainController
import id.yoframework.core.extension.logger.logger
import id.yoframework.web.extension.startHttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.kotlin.coroutines.CoroutineVerticle
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
class MainVerticle @Inject constructor(
    private val mainController: MainController
) : CoroutineVerticle() {

    private val serverName = "Boilerplate Backend"
    private val log = logger(MainVerticle::class)

    private fun resolvePort(): Int {
        return config.getInteger("HTTP_PORT")
    }

    override suspend fun start() {
        try {
            val router = mainController.create()
            val port = resolvePort()
            log.info("Starting $serverName on port $port")

            val options = HttpServerOptions().apply {
                isCompressionSupported = true
                compressionLevel = 7
            }

            val httpServer = vertx.startHttpServer(router, port, options)
            log.info("$serverName started on port ${httpServer.actualPort()}")
        } catch (ex: Exception) {
            log.error("Error when start HTTP server = ${ex.message}", ex)
        }
    }
}
