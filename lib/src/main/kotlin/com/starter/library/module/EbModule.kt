package com.starter.library.module

import arrow.core.getOrElse
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.getTry
import id.yoframework.core.module.CoreModule
import id.yoframework.db.createHikariPoolDataSource
import io.ebean.Database
import io.ebean.DatabaseFactory
import io.ebean.config.DatabaseConfig
import io.vertx.core.json.JsonObject
import javax.inject.Named
import javax.inject.Singleton
import javax.sql.DataSource

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi, Deny Prasetyo
 * @email danu.argi@gmail.com, jasoet87@gmail.com
 */

@Module(includes = [CoreModule::class])
class EBeanModule {

    @Provides
    @Singleton
    @Named("databaseUser")
    fun username(config: JsonObject): String {
        val key = "DB_USER"
        return config.getTry<String>(key).getOrElse { throw it }
    }

    @Provides
    @Singleton
    @Named("databasePassword")
    fun password(config: JsonObject): String {
        val key = "DB_PASSWORD"
        return config.getTry<String>(key).getOrElse { throw it }
    }

    @Provides
    @Singleton
    @Named("databaseUrl")
    fun url(config: JsonObject): String {
        val key = "DB_URL"
        return config.getTry<String>(key).getOrElse { throw it }
    }

    @Provides
    @Singleton
    @Named("databaseDriver")
    fun driver(config: JsonObject): String {
        val key = "DB_DRIVER_CLASSNAME"
        return config.getTry<String>(key).getOrElse { throw it }
    }

    @Provides
    @Singleton
    fun dataSource(
        @Named("databaseUser") user: String,
        @Named("databasePassword") password: String,
        @Named("databaseUrl") url: String,
        @Named("databaseDriver") driver: String
    ): DataSource {
        return createHikariPoolDataSource(
            name = "HikariPool",
            url = url,
            username = user,
            password = password,
            driver = driver
        )
    }

    @Provides
    @Singleton
    fun ebeanServer(dataSource: DataSource): Database {
        val config = DatabaseConfig().setName("ebeands").setDataSource(dataSource)
        return DatabaseFactory.create(config)
    }
}

