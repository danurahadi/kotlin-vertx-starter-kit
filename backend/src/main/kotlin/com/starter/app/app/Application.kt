package com.starter.app.app

import com.starter.app.app.module.DaggerAppComponent
import com.starter.app.app.module.DbModule
import com.starter.library.extension.generateMigrationFile
import com.starter.library.module.EBeanModule
import com.starter.library.module.EnvModule
import id.yoframework.core.extension.config.jsonConfig
import id.yoframework.core.extension.config.retrieveConfig
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import id.yoframework.core.extension.vertx.buildVertx
import id.yoframework.core.module.CoreModule
import id.yoframework.db.createHikariPoolDataSource
import id.yoframework.web.module.WebModule
import io.ebean.annotation.Platform
import io.vertx.core.DeploymentOptions
import io.vertx.core.ThreadingModel
import kotlinx.coroutines.*
import org.flywaydb.core.Flyway
import kotlin.system.exitProcess

/**
 * Main class as the entry point of this BE application
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
object Application {
    private const val DELAY_TIME = 2000L
//    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {

            val log = logger(Application::class)
            try {
                // At application startup, before any validation
//                System.setProperty("jakarta.el.ExpressionFactory", "org.glassfish.expressionlang.ExpressionFactoryImpl")

                val vertx = buildVertx()
                val config = vertx.retrieveConfig(jsonConfig("application-config.json"))

                val (app, buildTime) = executeTimeMillis {
                    DaggerAppComponent.builder()
                        .coreModule(CoreModule(config, vertx))
                        .envModule(EnvModule())
                        .dbModule(DbModule())
                        .webModule(WebModule())
                        .eBeanModule(EBeanModule())
                        .build()
                }

                log.info("Component Created takes $buildTime ms")

                val deployMainDef = async {
                    log.info("Deploy Main Verticle")
                    val (_, mainVertTime) = executeTimeMillis {
                        val mainVerticle = app.mainVerticle()
                        vertx.deployVerticle(mainVerticle, DeploymentOptions().apply {
                            this.config = config
                            this.threadingModel = ThreadingModel.EVENT_LOOP
                        })
                    }
                    log.info("Main Verticle Deployed takes $mainVertTime ms")
                }

                deployMainDef.await()

                val deployWorkerDef = async {
                    log.info("Deploy Worker Verticle")
                    val (_, workerVertTime) = executeTimeMillis {
                        val workerVerticle = app.workerVerticle()
                        vertx.deployVerticle(workerVerticle, DeploymentOptions().apply {
                            this.config = config
                            this.threadingModel = ThreadingModel.WORKER
                        })
                    }
                    log.info("Worker Verticle Deployed takes $workerVertTime ms")
                }

                val (_, migrationTime) = executeTimeMillis {
                    val ebServer = app.ebeanDatabase()

                    val currentDbMigrationName = app.currentDbMigrationName()
                    val currentDbMigrationVersion = app.currentDbMigrationVersion()
                    val pendingDropsFor = app.currentDbMigrationDropsFor()

                    System.setProperty("ddl.migration.version", currentDbMigrationVersion)
                    System.setProperty("ddl.migration.name", currentDbMigrationName)

                    if (pendingDropsFor.isNotBlank()) {
                        System.setProperty("ddl.migration.pendingDropsFor", pendingDropsFor)
                    }

                    ebServer.generateMigrationFile(
                        platform = Platform.POSTGRES,
                        prefix = "postgres"
                    )
                }
                log.info("Generate DB Migration files takes $migrationTime ms")

                // Create the Flyway instance and point it to the database
                val dbUrl = app.databaseUrl()
                val dbUser = app.flywayDBUser()
                val dbPassword = app.flywayDBPassword()

                val hikariCpDs = createHikariPoolDataSource(
                    name = "hikariCpDs",
                    url = dbUrl,
                    username = dbUser,
                    password = dbPassword,
                    driver = "org.postgresql.Driver"
                )

                val flyway = Flyway.configure().apply {
                    dataSource(hikariCpDs)
                    locations("dbmigration/postgres")
                    baselineVersion("0.1")
                }
                    .load()

//                flyway.baseline()

                // Start the migration
                val migrateResp = flyway.migrate()
                log.info("Flyway migration response : ${migrateResp.migrationsExecuted} migrations executed")

                delay(DELAY_TIME)
                hikariCpDs.close()

                val enableDataInitializer = app.enableDataInitializer()
                if (enableDataInitializer) {
                    val (_, dataInitTime) = executeTimeMillis {
                        val tempDataInit = app.tempDataInit()
                        tempDataInit()
                    }
                    log.info("Run data initializer takes $dataInitTime ms")
                }

                val disableScheduler = app.disableScheduler()
                if (!disableScheduler) {
                    val (_, schedulerTime) = executeTimeMillis {
                        val tempFileJobScheduler = app.tempFileJobScheduler()
                        tempFileJobScheduler.start()

                        val autoUnlockUserJobScheduler = app.autoUnlockUserJobScheduler()
                        autoUnlockUserJobScheduler.start()
                    }

                    log.info("Start Job Scheduler takes $schedulerTime ms")
                }

                deployWorkerDef.await()
            } catch (e: Exception) {
                log.error("${e.message} occurred while starting application", e)
                exitProcess(1)
            }
        }
//    }
}
