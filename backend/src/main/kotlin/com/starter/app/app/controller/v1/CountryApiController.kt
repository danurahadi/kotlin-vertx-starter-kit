package com.starter.app.app.controller.v1

import com.starter.app.app.service.SecurityService
import com.starter.app.domain.setting.handler.CountryHandler
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
 * Router class for Countries resources
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class CountryApiController @Inject constructor(
    override val router: Router,
    private val securityService: SecurityService,
    private val countryHandler: CountryHandler,
) : Controller({


    get("/all").jsonAsyncHandler {
//        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "countries.all")
        countryHandler.getAllCountries()
    }

    route("/*").asyncHandler {

        val accessToken = this.getAuthorizationHeader()
        val user = securityService.authenticateToken(accessToken, this)

        val roleName = user.principal().get<String>("roleName") orForbidden "Invalid user data."
        val identity = user.principal().get<String>("identity") orForbidden "Invalid user data."

        this.put("roleName", roleName)
        this.put("identity", identity)

        this.next()

    }
//
//    post("/").jsonAsyncHandler {
//        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "countries.create")
//        countryHandler.createCountry(this)
//    }
//

//    get("/:id").jsonAsyncHandler {
//        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "countries.details")
//        countryHandler.getCountryDetails(this)
//    }
//
//    put("/:id").jsonAsyncHandler {
//        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "countries.update")
//        countryHandler.updateCountry(this)
//    }
//    delete("/:id").jsonAsyncHandler {
//        securityService.isUserAuthorized(userRole = this.get("roleName"), accessName = "countries.delete")
//        countryHandler.deleteCountry(this)
//    }
})
