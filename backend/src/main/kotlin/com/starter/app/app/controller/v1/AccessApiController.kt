package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.auth.handler.AccessHandler
import com.starter.app.domain.auth.handler.AccessRoleHandler
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
 * Router class for Access resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@DelicateCoroutinesApi
class AccessApiController @Inject constructor(
    override val router: Router,
    private val securityService: SecurityService,
    private val accessHandler: AccessHandler,
    private val accessRoleHandler: AccessRoleHandler
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
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.create")
        accessHandler.createAccess(this)
    }

    get("/autocomplete").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.autocomplete")
        accessHandler.getAutocompleteAccess(this)
    }
    get("/").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.list")
        accessHandler.getAccessList(this)
    }

    get("/:accessId/roles/:roleId").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access-roles.details")
        accessRoleHandler.getAccessRoleDetails(this)
    }
    get("/:id/roles").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "roles.list.by.access")
        accessRoleHandler.getRoleListByAccessId(this)
    }
    get("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.details")
        accessHandler.getAccessDetails(this)
    }

    put("/:accessId/roles/:roleId/permission").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access-roles.permissions.update")
        accessRoleHandler.updateAccessRolePermission(this)
    }
    put("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.update")
        accessHandler.updateAccess(this)
    }

    delete("/:id").jsonAsyncHandler {
        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "access.delete")
        accessHandler.deleteAccess(this)
    }
})
