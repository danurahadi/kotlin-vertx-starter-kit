package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.auth.handler.AccessRoleHandler
import com.starter.app.domain.auth.handler.ModuleRoleHandler
import com.starter.app.domain.auth.handler.RoleHandler
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
 * Router class for Roles resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@DelicateCoroutinesApi
class RoleApiController @Inject constructor(
    override val router: Router,
    private val securityService: SecurityService,
    private val roleHandler: RoleHandler,
    private val accessRoleHandler: AccessRoleHandler,
    private val moduleRoleHandler: ModuleRoleHandler
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
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.create")
        roleHandler.createRole(this)
    }

    get("/autocomplete").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.autocomplete")
        roleHandler.getAutocompleteRoles(this)
    }
    get("/").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.list")
        roleHandler.getRoleList(this)
    }

    get("/:id/access").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.list.by.role")
        accessRoleHandler.getAccessListByRoleId(this)
    }
    get("/:id/modules").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.list.by.role")
        moduleRoleHandler.getModuleListByRoleId(this)
    }
    get("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.details")
        roleHandler.getRoleDetails(this)
    }

    put("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.update")
        roleHandler.updateRole(this)
    }
    delete("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.delete")
        roleHandler.deleteRole(this)
    }
})
