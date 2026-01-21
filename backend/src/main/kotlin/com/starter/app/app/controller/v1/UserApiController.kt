package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.user.handler.CmsUserHandler
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
 * Router class for Users resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class UserApiController @Inject constructor(
    override val router: Router,
    private val cmsUserHandler: CmsUserHandler,
    private val securityService: SecurityService
) : Controller({

    route("/*").asyncHandler {

        val accessToken = this.getAuthorizationHeader()
        val user = securityService.authenticateToken(accessToken, this)

        val roleName = user.principal().get<String>("roleName") orForbidden "Invalid user data."
        val identity = user.principal().get<String>("identity") orForbidden "Invalid user data."

        this.put("roleName", roleName)
        this.put("identity", identity)

        this.next()

    }

    get("/search").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.search")
        cmsUserHandler.searchUser(this)
    }
    
    put("/:id/email").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.email.change")
        cmsUserHandler.changeEmail(this)
    }
    put("/:id/username").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.username.change")
        cmsUserHandler.changeUsername(this)
    }

    put("/:id/phone").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.phone.change")
        cmsUserHandler.changePhoneNumber(this)
    }
    put("/:id/password").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.password.change")
        cmsUserHandler.changePassword(this)
    }

    put("/:id/unlock").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.unlock")
        cmsUserHandler.unlockAccount(this)
    }
    put("/:id/suspend").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.suspend")
        cmsUserHandler.suspendAccount(this)
    }
    put("/:id/reactivate").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.reactivate")
        cmsUserHandler.reactivateAccount(this)
    }
    put("/:id/settings").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.settings.update")
        cmsUserHandler.updateSettings(this)
    }

    delete("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "accounts.delete")
//        cmsUserHandler.deleteAccount(this)
    }
})
