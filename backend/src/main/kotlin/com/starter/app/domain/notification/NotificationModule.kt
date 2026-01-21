package com.starter.app.domain.notification

import com.starter.app.domain.admin.AdminModule
import dagger.Module
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

/**
 * Class that represent Notification Module and will provide some value, such as from config file.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
@Module(
    includes = [
        AdminModule::class
    ]
)
class NotificationModule
