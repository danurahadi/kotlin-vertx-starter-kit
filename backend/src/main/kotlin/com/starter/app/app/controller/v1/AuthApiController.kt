package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.auth.handler.UserAuthenticationHandler
import com.starter.library.extension.getAuthorizationHeader
import id.yoframework.core.json.get
import id.yoframework.web.controller.Controller
import id.yoframework.web.exception.orForbidden
import id.yoframework.web.extension.asyncHandler
import id.yoframework.web.extension.jsonAsyncHandler
import io.vertx.ext.web.Router
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router class for Auth resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AuthApiController @Inject constructor(
    override val router: Router,
    private val userAuthenticationHandler: UserAuthenticationHandler,
    private val securityService: SecurityService
) : Controller({

    post("/login").jsonAsyncHandler { userAuthenticationHandler.login(this) }
    post("/login/verify").jsonAsyncHandler { userAuthenticationHandler.loginVerification(this) }
    post("/login/verification-code").jsonAsyncHandler { userAuthenticationHandler.sendNewLogin2FACode(this) }

    post("/password/forgot").jsonAsyncHandler { userAuthenticationHandler.forgotPassword(this) }
    post("/password/reset").jsonAsyncHandler { userAuthenticationHandler.resetPassword(this) }

    post("/token/email").jsonAsyncHandler { userAuthenticationHandler.sendNewTokenEmail(this) }
    put("/email/verify").jsonAsyncHandler { userAuthenticationHandler.verifyEmail(this) }

    route("/*").asyncHandler {

        val accessToken = this.getAuthorizationHeader()
        val user = securityService.authenticateToken(accessToken, this)

        val roleName = user.principal().get<String>("roleName") orForbidden "Invalid user data."
        val identity = user.principal().get<String>("identity") orForbidden "Invalid user data."

        this.put("roleName", roleName)
        this.put("identity", identity)

        this.next()

    }

    post("/token/phone").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "auth.token-phone.new")
        userAuthenticationHandler.sendNewTokenPhone(this)
    }
    put("/phone/verify").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "auth.phone.verify")
        userAuthenticationHandler.verifyPhoneNumber(this)
    }
    delete("/logout").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "auth.logout")
        userAuthenticationHandler.logout(this)
    }
})
