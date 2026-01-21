package com.starter.app.domain.auth.plain

import com.starter.app.domain.auth.db.model.value.AccessRolePermission

/**
 * DTO class for wire response of GET access role details API to the client
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AccessRoleDetail(
    val access: AccessDetail,
    val role: RoleDetail,
    val permission: AccessRolePermission,
    val lastUpdatedAt: String
)
