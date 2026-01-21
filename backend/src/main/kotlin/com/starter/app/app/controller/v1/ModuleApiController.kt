package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.auth.handler.ModuleHandler
import com.starter.app.domain.auth.handler.ModuleRoleHandler
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
 * Router class for Modules resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@DelicateCoroutinesApi
class ModuleApiController @Inject constructor(
    override val router: Router,
    private val securityService: SecurityService,
    private val moduleHandler: ModuleHandler,
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
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.create")
        moduleHandler.createModule(this)
    }

    get("/all").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.all")
        moduleHandler.getAllModules()
    }
    get("/").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.list")
        moduleHandler.getModuleList(this)
    }
    get("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.details")
        moduleHandler.getModuleDetails(this)
    }

    put("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.update")
        moduleHandler.updateModule(this)
    }
    delete("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "modules.delete")
        moduleHandler.deleteModule(this)
    }


    /**
     * Route for Roles sub-resource
     *
     */
    get("/:id/roles").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.list.by.module")
        moduleRoleHandler.getRoleListByModuleId(this)
    }
    get("/:moduleId/roles/:roleId").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "module-roles.details")
        moduleRoleHandler.getModuleRoleDetails(this)
    }
    put("/:moduleId/roles/:roleId/permission").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "module-roles.permissions.update")
        moduleRoleHandler.updateModuleRolePermission(this)
    }
})
