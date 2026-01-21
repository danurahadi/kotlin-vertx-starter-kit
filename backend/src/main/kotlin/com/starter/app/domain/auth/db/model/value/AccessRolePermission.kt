package com.starter.app.domain.auth.db.model.value

import io.ebean.annotation.EnumValue

/**
 * Enum class that represent the permission given to each Role for each Access (API endpoint)
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

enum class AccessRolePermission {
    @EnumValue(value = "ALLOWED")
    ALLOWED,

    @EnumValue(value = "DENIED")
    DENIED
}
