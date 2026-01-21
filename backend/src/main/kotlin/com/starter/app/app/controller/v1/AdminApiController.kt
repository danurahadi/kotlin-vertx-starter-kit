package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.admin.handler.AdminHandler
import com.starter.app.domain.notification.handler.AdminNotificationHandler
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
 * Router class for Admins resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AdminApiController @Inject constructor(
    override val router: Router,
    private val securityService: SecurityService,
    private val adminHandler: AdminHandler,
    private val adminNotificationHandler: AdminNotificationHandler
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

    post("/").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.create")
        adminHandler.createAdmin(this)
    }

    get("/").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.list")
        adminHandler.getAdminList(this)
    }
    get("/dashboard/general").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.dashboard.general")
        adminHandler.getGeneralInfoDashboard(this)
    }
    get("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.details")
        adminHandler.getProfile(this)
    }

    put("/:id/picture").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.picture.upload")
        adminHandler.uploadProfilePicture(this)
    }
    put("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.update")
        adminHandler.updateProfile(this)
    }


    /**
     * Route for Notifications sub-resource
     *
     */
    get("/:id/notifications").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.notifications.list")
        adminNotificationHandler.getAdminNotificationList(this)
    }
    patch("/:id/notifications/read_all").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.notifications.read-all")
        adminNotificationHandler.readAllAdminNotifications(this)
    }

    delete("/:id/notifications/clear").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.notifications.clear")
        adminNotificationHandler.clearAdminNotifications(this)
    }
    delete("/:adminId/notifications/:notificationId").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "admins.notifications.delete")
        adminNotificationHandler.deleteAdminNotification(this)
    }
})
