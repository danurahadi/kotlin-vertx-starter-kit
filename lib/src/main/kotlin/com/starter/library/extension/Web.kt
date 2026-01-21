package com.starter.library.extension

import id.yoframework.web.exception.SecurityException
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.coAwait

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 */

fun RoutingContext.getAuthorizationHeader(): String {
    return this.request().headers().get("X-Access-Token") ?:
        throw SecurityException("Access token header is required.")
}

fun RoutingContext.getCustomAPIKeyHeader(): String {
    return this.request().headers().get("X-API-Key") ?:
        throw SecurityException("API Key header is required.")
}

suspend fun Vertx.createHttpServerWithOptions(port: Int, router: Router): HttpServer {
    val options = HttpServerOptions().apply {
        isCompressionSupported = true
        compressionLevel = 7
    }
    val httpServer = this.createHttpServer(options)
        .requestHandler(router)
    return httpServer.listen(port).coAwait()
}
