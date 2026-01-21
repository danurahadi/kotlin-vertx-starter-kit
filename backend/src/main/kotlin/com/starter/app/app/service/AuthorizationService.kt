package com.starter.app.app.service

import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.user.db.model.CmsUser
import com.starter.app.domain.user.db.repository.CmsUserRepository
import id.yoframework.web.exception.InvalidCredentials
import id.yoframework.web.exception.orDataError
import id.yoframework.web.exception.orUnauthorized
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class AuthorizationService @Inject constructor(private val cmsUserRepository: CmsUserRepository) {

    suspend fun authorizeUser(identity: String, userId: String? = null): CmsUser {
        val loggedInUser = cmsUserRepository.findByIdentity(identity = identity)
            .findOne() orUnauthorized "Invalid access token."

        if (userId != null && userId != loggedInUser.externalId) {
            throw InvalidCredentials("You are not authorized to access this feature.")
        }
        return loggedInUser
    }

    suspend fun authorizeAdmin(identity: String, adminId: String? = null): Admin {
        val loggedInUser = cmsUserRepository.findByIdentity(identity = identity)
            .findOne() orUnauthorized "Invalid access token."

        val loggedInAdmin = loggedInUser.admin orDataError "Invalid user data."
        if (adminId != null && adminId != loggedInAdmin.externalId) {
            throw InvalidCredentials("You are not authorized to access this feature.")
        }
        return loggedInAdmin
    }
}

