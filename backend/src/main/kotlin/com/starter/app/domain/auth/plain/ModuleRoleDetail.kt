package com.starter.app.domain.auth.plain

import com.starter.app.domain.auth.db.model.value.AccessRolePermission

/**
 * DTO class for wire response of GET module role details API to the client
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ModuleRoleDetail(
    val module: ModuleDetail,
    val role: RoleDetail,
    val permission: AccessRolePermission,
    val lastUpdatedAt: String
)
