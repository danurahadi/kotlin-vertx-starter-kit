package com.starter.app.app.module

import com.starter.app.app.verticle.MainVerticle
import com.starter.app.app.verticle.WorkerVerticle
import com.starter.app.domain.admin.AdminModule
import com.starter.app.domain.auth.AuthModule
import com.starter.app.domain.notification.NotificationModule
import com.starter.app.domain.setting.SettingModule
import com.starter.app.domain.user.UserModule
import com.starter.app.integration.eventbus.EventBusModule
import com.starter.app.integration.fileupload.FileUploadModule
import com.starter.app.integration.fileupload.aws.AwsS3Module
import com.starter.app.integration.firebase.FirebaseModule
import com.starter.app.integration.google.GoogleModule
import com.starter.app.integration.sms.SmsModule
import com.starter.app.scheduler.SchedulerModule
import com.starter.library.module.EBeanModule
import com.starter.library.module.EnvModule
import dagger.Component
import id.yoframework.core.module.CoreModule
import id.yoframework.web.module.PebbleModule
import id.yoframework.web.module.WebModule
import io.ebean.Database
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.quartz.Scheduler
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
@Component(
    modules = [
        CoreModule::class, WebModule::class, EBeanModule::class, PebbleModule::class,
        DbModule::class, EnvModule::class, AppDsModule::class, AwsS3Module::class,
        FileUploadModule::class, SchedulerModule::class, EventBusModule::class,
        SettingModule::class, FirebaseModule::class, AuthModule::class, UserModule::class,
        AdminModule::class, GoogleModule::class, NotificationModule::class, SmsModule::class
    ]
)
interface AppComponent {
    fun mainVerticle(): MainVerticle
    fun workerVerticle(): WorkerVerticle
    @Named("dataInitializer")
    fun dataInitializer(): suspend () -> Unit
    @Named("tempDataInit")
    fun tempDataInit(): suspend () -> Unit
    @Named("enableDataInitializer")
    fun enableDataInitializer(): Boolean
    @Named("disableScheduler")
    fun disableScheduler(): Boolean
    @Named("tempFileJobScheduler")
    fun tempFileJobScheduler(): Scheduler
    @Named("autoUnlockUserJobScheduler")
    fun autoUnlockUserJobScheduler(): Scheduler
    fun ebeanDatabase(): Database
    @Named("flywayDBUser")
    fun flywayDBUser(): String
    @Named("flywayDBPassword")
    fun flywayDBPassword(): String
    @Named("databaseUrl")
    fun databaseUrl(): String
    @Named("currentDbMigrationName")
    fun currentDbMigrationName(): String
    @Named("currentDbMigrationVersion")
    fun currentDbMigrationVersion(): String
    @Named("currentDbMigrationDropsFor")
    fun currentDbMigrationDropsFor(): String
}
