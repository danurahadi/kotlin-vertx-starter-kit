package com.starter.app.app.module

import com.starter.app.domain.admin.db.model.ActivityLog
import com.starter.app.domain.admin.db.model.Admin
import com.starter.app.domain.admin.db.repository.ActivityLogRepository
import com.starter.app.domain.admin.db.repository.AdminRepository
import com.starter.app.domain.auth.db.model.Access
import com.starter.app.domain.auth.db.model.AccessRole
import com.starter.app.domain.auth.db.model.ModuleRole
import com.starter.app.domain.auth.db.model.Role
import com.starter.app.domain.auth.db.model.value.AccessRolePermission
import com.starter.app.domain.auth.db.repository.*
import com.starter.app.domain.notification.db.model.AdminNotification
import com.starter.app.domain.notification.db.repository.AdminNotificationRepository
import com.starter.app.domain.user.db.model.CmsUser
import com.starter.app.domain.user.db.model.CmsUserSetting
import com.starter.app.domain.user.db.model.value.CmsUserStatus
import com.starter.app.domain.user.db.repository.CmsUserRepository
import com.starter.app.domain.user.db.repository.CmsUserSettingRepository
import com.starter.library.module.EBeanModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.json.getExcept
import id.yoframework.extra.extension.password.encode
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.util.*
import javax.inject.Named
import javax.inject.Singleton
import com.starter.app.domain.auth.db.model.Module as Modules

/**
 * [Documentation Here]
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
@Module(includes = [EBeanModule::class])
class AppDsModule {

    private val log = logger<AppDsModule>()

    @Provides
    @Singleton
    @Named("usedEnv")
    fun usedEnv(config: JsonObject): String {
        return try {
            config.getExcept("ENV")
        } catch (e: Exception) {
            log.error("${e.message} when Provides Env config")
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("enableDataInitializer")
    fun enableDataInitializer(config: JsonObject): Boolean {
        return try {
            config.getExcept("ENABLE_DATA_INITIALIZER")
        } catch (e: Exception) {
            log.error("${e.message} when Provides Enable Data Initializer config")
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("tempDataInit")
    fun tempDataInit(
        accessRepository: AccessRepository,
        roleRepository: RoleRepository,
        accessRoleRepository: AccessRoleRepository,
        moduleRepository: ModuleRepository,
//        cmsUserRepository: CmsUserRepository,
//        cmsUserSettingRepository: CmsUserSettingRepository,
//        adminRepository: AdminRepository,
//        adminNotificationRepository: AdminNotificationRepository,
//        activityLogRepository: ActivityLogRepository
    ): suspend () -> Unit {
        return {

            val coreModule = moduleRepository.findAll().find { it.name == "Core" }
            val newAccess = listOf(
                Access(
                    module = coreModule,
                    name = "activity.logs",
                    alias = "Get Activity Logs"
                ),
            )

            val accessNames = newAccess.map { it.name }
            val existingNames = accessRepository.findByNames(accessNames).findCount()

            if (existingNames == 0) {
                val roles = roleRepository.findAll()
                val accessRoles = roles.flatMap { r ->

                    val permission = if (r.name == "SUPERADMIN") {
                        AccessRolePermission.ALLOWED
                    } else AccessRolePermission.DENIED

                    newAccess.map { a ->
                        AccessRole(
                            access = a,
                            role = r,
                            permission = permission
                        )
                    }
                }
                accessRepository.insertAll(newAccess)
                accessRoleRepository.insertAll(accessRoles)
            }
        }
    }

    @Provides
    @Singleton
    @Named("dataInitializer")
    fun dataInitializer(
        accessRepository: AccessRepository,
        roleRepository: RoleRepository,
        accessRoleRepository: AccessRoleRepository,
        moduleRepository: ModuleRepository,
        moduleRoleRepository: ModuleRoleRepository,
        cmsUserRepository: CmsUserRepository,
        cmsUserSettingRepository: CmsUserSettingRepository,
        adminRepository: AdminRepository,
        adminNotificationRepository: AdminNotificationRepository,
        activityLogRepository: ActivityLogRepository
    ): suspend () -> Unit {
        return {

            val roles = listOf(
                Role(
                    name = "SUPERADMIN",
                    alias = "Superadmin",
                    description = "Role for Super Admin User Group"
                ),
                Role(
                    name = "ADMIN",
                    alias = "Admin",
                    description = "Role for Admin User Group"
                )
            )
            val modules = listOf(
                Modules(
                    code = "CR",
                    name = "Core",
                    summary = "Module for basic features like auth, user, & settings"
                ),
            )
            val accessList = listOf(
                Access(
                    module = modules[0],
                    name = "access.create",
                    alias = "Create Access"
                ),
                Access(
                    module = modules[0],
                    name = "access.list",
                    alias = "Get Access List"
                ),
                Access(
                    module = modules[0],
                    name = "access.autocomplete",
                    alias = "Get Autocomplete Access"
                ),
                Access(
                    module = modules[0],
                    name = "access.details",
                    alias = "Get Access Details"
                ),
                Access(
                    module = modules[0],
                    name = "access.update",
                    alias = "Update Access"
                ),
                Access(
                    module = modules[0],
                    name = "access.delete",
                    alias = "Delete Access"
                ),
                Access(
                    module = modules[0],
                    name = "access-roles.details",
                    alias = "Get Access Role Details"
                ),
                Access(
                    module = modules[0],
                    name = "roles.list.by.access",
                    alias = "Get Role List By Access"
                ),
                Access(
                    module = modules[0],
                    name = "access-roles.permissions.update",
                    alias = "Update Access Role Permission"
                ),
                Access(
                    module = modules[0],
                    name = "admins.create",
                    alias = "Create Admin"
                ),
                Access(
                    module = modules[0],
                    name = "admins.list",
                    alias = "Get Admin List"
                ),
                Access(
                    module = modules[0],
                    name = "admins.dashboard.general",
                    alias = "Get Admin Dashboard Info"
                ),
                Access(
                    module = modules[0],
                    name = "admins.details",
                    alias = "Get Admin Profile"
                ),
                Access(
                    module = modules[0],
                    name = "admins.picture.upload",
                    alias = "Upload Admin Picture"
                ),
                Access(
                    module = modules[0],
                    name = "admins.update",
                    alias = "Update Admin Profile"
                ),
                Access(
                    module = modules[0],
                    name = "admins.notifications.list",
                    alias = "Get Admin Notification List"
                ),
                Access(
                    module = modules[0],
                    name = "admins.notifications.read-all",
                    alias = "Read All Admin Notifications"
                ),
                Access(
                    module = modules[0],
                    name = "admins.notifications.clear",
                    alias = "Clear Admin Notifications"
                ),
                Access(
                    module = modules[0],
                    name = "admins.notifications.delete",
                    alias = "Delete Admin Notification"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.create",
                    alias = "Create App Settings"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.list",
                    alias = "Get App Setting List"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.all",
                    alias = "Get All App Settings"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.details",
                    alias = "Get App Setting Details"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.bulk-update",
                    alias = "Bulk Update App Settings"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.update",
                    alias = "Update App Setting"
                ),
                Access(
                    module = modules[0],
                    name = "app-settings.delete",
                    alias = "Delete App Setting"
                ),
                Access(
                    module = modules[0],
                    name = "auth.token-phone.new",
                    alias = "Send New Token Phone"
                ),
                Access(
                    module = modules[0],
                    name = "auth.phone.verify",
                    alias = "Verify Phone Number"
                ),
                Access(
                    module = modules[0],
                    name = "auth.logout",
                    alias = " Logout User"
                ),
                Access(
                    module = modules[0],
                    name = "modules.create",
                    alias = "Create Module"
                ),
                Access(
                    module = modules[0],
                    name = "modules.list",
                    alias = "Get Module List"
                ),
                Access(
                    module = modules[0],
                    name = "modules.all",
                    alias = "Get All Modules"
                ),
                Access(
                    module = modules[0],
                    name = "modules.details",
                    alias = "Get Module Details"
                ),
                Access(
                    module = modules[0],
                    name = "modules.update",
                    alias = "Update Module"
                ),
                Access(
                    module = modules[0],
                    name = "modules.delete",
                    alias = "Delete Module"
                ),
                Access(
                    module = modules[0],
                    name = "roles.list.by.module",
                    alias = "Get Role List By Module"
                ),
                Access(
                    module = modules[0],
                    name = "module-roles.details",
                    alias = "Get Module Role Details"
                ),
                Access(
                    module = modules[0],
                    name = "module-roles.permissions.update",
                    alias = "Update Module Role Permission"
                ),
                Access(
                    module = modules[0],
                    name = "roles.create",
                    alias = "Create Role"
                ),
                Access(
                    module = modules[0],
                    name = "roles.autocomplete",
                    alias = "Get Autocomplete Roles"
                ),
                Access(
                    module = modules[0],
                    name = "roles.list",
                    alias = "Get Role List"
                ),
                Access(
                    module = modules[0],
                    name = "roles.details",
                    alias = "Get Role Details"
                ),
                Access(
                    module = modules[0],
                    name = "roles.update",
                    alias = "Update Role"
                ),
                Access(
                    module = modules[0],
                    name = "roles.delete",
                    alias = "Delete Role"
                ),
                Access(
                    module = modules[0],
                    name = "access.list.by.role",
                    alias = "Get Access List By Role"
                ),
                Access(
                    module = modules[0],
                    name = "modules.list.by.role",
                    alias = "Get Module List By Role"
                ),
                Access(
                    module = modules[0],
                    name = "setting-groups.create",
                    alias = "Create Setting Group"
                ),
                Access(
                    module = modules[0],
                    name = "setting-groups.list",
                    alias = "Get Setting Group List"
                ),
                Access(
                    module = modules[0],
                    name = "setting-groups.all",
                    alias = "Get All Setting Groups"
                ),
                Access(
                    module = modules[0],
                    name = "setting-groups.details",
                    alias = "Get Setting Group Details"
                ),
                Access(
                    module = modules[0],
                    name = "setting-groups.update",
                    alias = "Update Setting Group"
                ),
                Access(
                    module = modules[0],
                    name = "setting-groups.delete",
                    alias = "Delete Setting Group"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.search",
                    alias = "Search Users"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.email.change",
                    alias = "Change Email"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.username.change",
                    alias = "Change Username"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.phone.change",
                    alias = "Change Phone Number"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.password.change",
                    alias = "Change Password"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.suspend",
                    alias = "Suspend User"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.reactivate",
                    alias = "Reactivate User"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.unlock",
                    alias = "Unlock User"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.settings.update",
                    alias = "Update User Settings"
                ),
                Access(
                    module = modules[0],
                    name = "accounts.delete",
                    alias = "Delete User"
                ),
                Access(
                    module = modules[0],
                    name = "api-keys.create",
                    alias = "Create API key"
                ),
                Access(
                    module = modules[0],
                    name = "api-keys.list",
                    alias = "Get API key list"
                ),
                Access(
                    module = modules[0],
                    name = "api-keys.delete",
                    alias = "Delete API key"
                ),
                Access(
                    module = modules[0],
                    name = "activity.logs",
                    alias = "Get Activity Logs"
                ),
            )
            val moduleRoles = modules.flatMap { m ->
                roles.map { r ->
                    ModuleRole(
                        module = m,
                        role = r,
                        permission = AccessRolePermission.ALLOWED
                    )
                }
            }
            val accessRoles = accessList.flatMap { a ->
                roles.map { r ->
                    val permission = if (r.name == "ADMIN" && (a.name.contains("access")
                                || a.name.contains("admins")
                                || a.name.contains("roles"))) {
                        AccessRolePermission.DENIED
                    } else AccessRolePermission.ALLOWED

                    AccessRole(
                        access = a,
                        role = r,
                        permission = permission
                    )
                }
            }

            val users = mutableListOf<CmsUser>()
            roles.mapIndexed { index, role ->
                for (i in 1..5) {

                    val uniquePhone = index + 1
                    val roleName = role.name.lowercase()

                    val user = CmsUser(
                        role = role,
                        email = "$roleName.$i@mail.com",
                        username = "${roleName}_$i",
                        phone = "081${uniquePhone}0000000$i",
                        password = "Admin327E".encode(),
                        emailVerified = true,
                        status = CmsUserStatus.ACTIVE
                    )
                    users.add(user)
                }
            }

            val admins = users.map { u ->
                val isSuperAdmin = u.username.contains("superadmin")
                Admin(
                    cmsUser = u,
                    superadmin = isSuperAdmin,
                    fullName = u.username.replace("_", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                )
            }
            val userSettings = users.map { u ->
                CmsUserSetting(
                    cmsUser = u,
                    timezoneOffset = 420,
                )
            }

            roleRepository.insertAll(roles)
            moduleRepository.insertAll(modules)
            accessRepository.insertAll(accessList)

            moduleRoleRepository.insertAll(moduleRoles)
            accessRoleRepository.insertAll(accessRoles)

            cmsUserRepository.insertAll(users)
            adminRepository.insertAll(admins)
            cmsUserSettingRepository.insertAll(userSettings)

            val adminNotifs = mutableListOf<AdminNotification>()
            val activityLogs = mutableListOf<ActivityLog>()

            admins.map { a ->
                for (i in 1..100) {
                    val adminNotif = AdminNotification(
                        admin = a,
                        message = "Admin $i edited some data on <a href=\"/resources/id123\">Test Notif Link</a>."
                    )
                    adminNotifs.add(adminNotif)
                }
            }
            adminNotificationRepository.insertAll(adminNotifs)

            admins.map { a ->
                for (i in 1..100) {
                    val activityLog = ActivityLog(
                        actor = a,
                        description = "Admin $i added a new data to <a href=\"/resources/id\">Test Activity Log Link</a>."
                    )
                    activityLogs.add(activityLog)
                }
            }
            activityLogRepository.insertAll(activityLogs)
        }
    }
}
