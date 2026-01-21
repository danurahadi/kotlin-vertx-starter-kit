package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.setting.handler.AppSettingHandler
import com.starter.app.domain.setting.handler.SettingGroupHandler
import com.starter.library.extension.getAuthorizationHeader
import id.yoframework.core.json.get
import id.yoframework.web.controller.Controller
import id.yoframework.web.exception.orForbidden
import id.yoframework.web.extension.asyncHandler
import id.yoframework.web.extension.jsonAsyncHandler
import io.vertx.ext.web.Router
import kotlinx.coroutines.DelicateCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Router class for App Settings resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@DelicateCoroutinesApi
class AppSettingApiController @Inject constructor(
    override val router: Router,
    private val securityService: SecurityService,
    private val appSettingHandler: AppSettingHandler,
    private val settingGroupHandler: SettingGroupHandler
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
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.create")
        appSettingHandler.createAppSetting(this)
    }

    get("/").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.list")
        appSettingHandler.getAppSettingList(this)
    }
    get("/all").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.all")
        settingGroupHandler.getAllSettingGroupsWithAppSettings()
    }

    get("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.details")
        appSettingHandler.getAppSettingDetailsByIdOrKey(this)
    }

    put("/bulk").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.bulk-update")
        appSettingHandler.bulkUpdateAppSettings(this)
    }
    put("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.update")
        appSettingHandler.updateAppSetting(this)
    }

    delete("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "app-settings.delete")
        appSettingHandler.deleteAppSetting(this)
    }
})
