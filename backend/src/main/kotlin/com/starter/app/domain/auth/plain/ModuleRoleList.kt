package com.starter.app.domain.auth.plain

import com.starter.app.domain.auth.db.model.value.AccessRolePermission

/**
 * DTO class for hold role list for each module data
 * alongside with the corresponding permission status
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

data class ModuleRoleList(
    val id: String,
    val name: String,
    val alias: String,
    val permission: AccessRolePermission,
    val lastUpdatedAt: String
)
