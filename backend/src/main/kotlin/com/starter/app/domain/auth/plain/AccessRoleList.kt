package com.starter.app.domain.auth.plain

import com.starter.app.domain.auth.db.model.value.AccessRolePermission

/**
 * DTO class for hold role list for each access data
 * alongside with the corresponding permission status
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

data class AccessRoleList(
    val id: String,
    val name: String,
    val alias: String,
    val permission: AccessRolePermission,
    val lastUpdatedAt: String
)
