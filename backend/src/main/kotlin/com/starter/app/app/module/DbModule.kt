package com.starter.app.app.module

import arrow.core.getOrElse
import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.json.getExcept
import id.yoframework.core.json.getTry
import io.vertx.core.json.JsonObject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(includes = [EnvModule::class])
class DbModule {

    private val log = logger<DbModule>()

    @Provides
    @Singleton
    @Named("currentDbMigrationName")
    fun currentDbMigrationName(config: JsonObject): String {
        return try {
            config.getExcept("CURRENT_DB_MIGRATION_NAME")
        } catch (e: Exception) {
            log.error("${e.message} when Provides Current DB Migration Name")
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("currentDbMigrationVersion")
    fun currentDbMigrationVersion(config: JsonObject): String {
        return try {
            config.getExcept("CURRENT_DB_MIGRATION_VERSION")
        } catch (e: Exception) {
            log.error("${e.message} when Provides Current DB Migration Version")
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("currentDbMigrationDropsFor")
    fun currentDbMigrationDropsFor(config: JsonObject): String {
        return try {
            config.getExcept("CURRENT_DB_MIGRATION_DROPS_FOR")
        } catch (e: Exception) {
            log.error("${e.message} when Provides Current DB Migration Drops For")
            throw e
        }
    }

    @Provides
    @Singleton
    @Named("flywayDBUser")
    fun flywayDBUser(config: JsonObject): String {
        val key = "FLYWAY_DB_USER"
        return config.getTry<String>(key).getOrElse { throw it }
    }

    @Provides
    @Singleton
    @Named("flywayDBPassword")
    fun flywayDBPassword(config: JsonObject): String {
        val key = "FLYWAY_DB_PASSWORD"
        return config.getTry<String>(key).getOrElse { throw it }
    }
}
