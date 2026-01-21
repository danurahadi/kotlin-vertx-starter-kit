package com.starter.library.module

import dagger.Module
import dagger.Provides
import id.yoframework.core.json.getExcept
import id.yoframework.core.module.CoreModule
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.redis.client.Redis
import io.vertx.redis.client.RedisOptions
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Deny Prasetyo.
 */

@Module(includes = [CoreModule::class, EnvModule::class])
class RedisModule {
    companion object {
        fun redisOptions(
            host: String = "localhost",
            port: Int = 6379,
            database: String = "",
            username: String = "",
            password: String = "",
        ): RedisOptions {
            val connString = "redis://$username:$password@$host:$port/$database"
            return RedisOptions().apply {
                setConnectionString(connString)
            }
        }
    }

    @Provides
    @Singleton
    @Named("redisConfig")
    fun redisConfig(config: JsonObject): RedisOptions {
        val redisHost: String = config.getExcept("REDIS_HOST")
        val redisPort = config.getInteger("REDIS_PORT", 6379)
        val redisDbName = config.getString("REDIS_DB_NAME")

        val redisUsername = config.getString("REDIS_USERNAME")
        val redisPassword = config.getString("REDIS_PASSWORD")
        return redisOptions(redisHost, redisPort, redisDbName, redisUsername, redisPassword)
    }

    @Provides
    @Singleton
    @Named("redisClient")
    fun redisClient(@Named("redisConfig") options: RedisOptions, vertx: Vertx): Redis {
        return Redis.createClient(vertx, options)
    }
}
