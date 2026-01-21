package com.starter.app.app.controller

import com.starter.app.app.controller.v1.*
import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.exception.NullObjectException
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.json.encodePrettily
import id.yoframework.web.controller.Controller
import id.yoframework.web.exception.*
import id.yoframework.web.extension.*
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.jsonObjectOf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import net.logstash.logback.marker.Markers
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Router class that mount API v1 controller class
 * and build some handlers such as Body Handler, CORS Handler, and SockJS Handler
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class MainController @Inject constructor(
    override val router: Router,
    private val vertx: Vertx,
    private val coreDevApiController: CoreDevApiController,
    private val authApiController: AuthApiController,
    private val accessApiController: AccessApiController,
    private val roleApiController: RoleApiController,
    private val adminApiController: AdminApiController,
    private val userApiController: UserApiController,
    private val moduleApiController: ModuleApiController,
    private val settingGroupApiController: SettingGroupApiController,
    private val appSettingApiController: AppSettingApiController,
    private val countryApiController: CountryApiController,
    private val activityLogApiController: ActivityLogApiController,
    private val apiKeyApiController: ApiKeyApiController,
    @param:Named("corsAllowedOrigin") private val corsAllowedOriginPattern: String
) : Controller({

    // event bus bridge
    val sockJSHandler = SockJSHandler.create(vertx)
    val options = SockJSBridgeOptions()
        .addInboundPermitted(PermittedOptions().setAddress("users.onlineStatus.update"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("users.onlineStatus.fetch\\..+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("resp.notifs.admins\\..+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("admins.notifications\\..+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("admins.unreadNotifsCount\\..+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("matches.statistics\\..+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("matches.list\\..+"))
        .addOutboundPermitted(PermittedOptions().setAddressRegex("matches.details\\..+"))

    val sockJSRouter = sockJSHandler.bridge(options)

    route().handler(BodyHandler.create())

    // mount event bus router as sub-router & append the handler
    route("/eventbus/*").subRouter(sockJSRouter)

    route().handler(
        CorsHandler.create()
            .addOriginWithRegex(corsAllowedOriginPattern)
            .allowedHeaders(setOf(
                "Accept", "Origin", "Content-Type", "X-Access-Token"
            ))
            .allowedMethods(setOf(
                HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS
            ))
    )

    val log = logger(MainController::class)
    get("/").jsonAsyncHandler {
        jsonObjectOf(
            "status" to "running",
            "version" to "1.0",
            "message" to "Welcome to JogjaNode Starter API v1"
        )
    }

    // core dev area
    subRoute("/core-dev*", coreDevApiController)

    subRoute("/access*", accessApiController)
    subRoute("/accounts*", userApiController)
    subRoute("/activity-logs*", activityLogApiController)

    subRoute("/admins*", adminApiController)
    subRoute("/api-keys*", apiKeyApiController)
    subRoute("/app-settings*", appSettingApiController)

    subRoute("/auth*", authApiController)
    subRoute("/countries*", countryApiController)
    subRoute("/modules*", moduleApiController)

    subRoute("/roles*", roleApiController)
    subRoute("/setting-groups*", settingGroupApiController)

    route().finalErrorHandler(object : ErrorHandler {

        override fun invoke(context: RoutingContext, e: Throwable) {

            val code = when (e) {
                is FileNotFoundException -> HttpResponseStatus.NOT_FOUND.code()
                is NullObjectException -> HttpResponseStatus.NOT_FOUND.code()
                is DataInconsistentException -> HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
                is NotAllowedException -> HttpResponseStatus.METHOD_NOT_ALLOWED.code()
                is SecurityException -> HttpResponseStatus.UNAUTHORIZED.code()
                is ValidationException -> HttpResponseStatus.UNPROCESSABLE_ENTITY.code()
                is BadRequestException -> HttpResponseStatus.BAD_REQUEST.code()
                is UnauthorizedException -> HttpResponseStatus.UNAUTHORIZED.code()
                is NotFoundException -> HttpResponseStatus.NOT_FOUND.code()
                is InvalidCredentials -> HttpResponseStatus.FORBIDDEN.code()
                else ->
                    if (context.statusCode() > 0) {
                        context.statusCode()
                    } else {
                        500
                    }
            }

            val message = if (code.toString().startsWith("5")) {
                "Service is unavailable for now. Please try again later."
            } else {
                e.message
            }
            val cause = e.cause?.message ?: e.message

            if (e is ValidationException) {
                log.error(
                    Markers.appendEntries(
                        mapOf(
                            "message" to e.errors,
                            "cause" to cause
                        )
                    ),
                    "Validation Exception"
                )
            } else {
                log.error(
                    Markers.appendEntries(
                        mapOf(
                            "message" to e.message,
                            "cause" to cause
                        )
                    ),
                    "Non-Validation Exception"
                )
            }

            val acceptHeader = context.header("Accept") ?: ""
            val contentTypeHeader = context.header("Content-Type") ?: ""

            if (acceptHeader.contains("/json") || contentTypeHeader.contains("/json")) {

                val result = if (e is ValidationException) {
                    mapOf(
                        "message" to e.errors.joinToString("<br>"),
                        "errors" to cause
                    )
                } else {
                    mapOf(
                        "message" to message,
                        "errors" to cause
                    )
                }

                context.response()
                    .setStatusCode(code)
                    .putHeader("Content-Type", "application/json; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", context.header("Origin") ?: "*")
                    .end(Json.encodePrettily(result))

            } else {

                val result = if (e is ValidationException) {
                    e.errors.firstOrNull()
                } else {
                    message
                }

                context.response()
                    .setStatusCode(code)
                    .putHeader("Content-Type", "text/plain; charset=utf-8")
                    .putHeader("Access-Control-Allow-Origin", context.header("Origin") ?: "*")
                    .end(result ?: "Something went wrong. Please try again later.")
            }
        }
    })
})
