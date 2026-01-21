package com.starter.app.domain.auth

import com.starter.app.integration.mailer.MailerModule
import com.starter.library.module.EBeanModule
import dagger.Module
import dagger.Provides
import id.yoframework.core.json.get
import id.yoframework.web.exception.orNotFound
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.kotlin.ext.auth.keyStoreOptionsOf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Named
import javax.inject.Singleton

/**
 * Class that represent Authentication Module and will provide some value, such as from config file.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
@Module(
    includes = [
        EBeanModule::class,
        MailerModule::class
    ]
)
class AuthModule {

    @Provides
    @Singleton
    @Named("jwtKeystorePath")
    fun jwtKeystorePath(config: JsonObject): String {
        val key = "JWT_AUTH_KEYSTORE_PATH"
        return config.get<String>(key) orNotFound "JWT Auth Keystore path config is required"
    }

    @Provides
    @Singleton
    @Named("jwtKeystorePassword")
    fun jwtKeystorePassword(config: JsonObject): String {
        val key = "JWT_AUTH_KEYSTORE_PASSWORD"
        return config.get<String>(key) orNotFound "JWT Auth Keystore password config is required"
    }

    @Provides
    @Singleton
    @Named("jwtExpiresInSeconds")
    fun jwtExpiresInSeconds(config: JsonObject): Int {
        val key = "JWT_AUTH_EXPIRES_IN_SECONDS"
        return config.get<Int>(key) orNotFound "JWT Auth Expires in seconds config is required"
    }

    @Provides
    @Singleton
    @Named("jwtAlgorithm")
    fun jwtAlgorithm(config: JsonObject): String {
        val key = "JWT_AUTH_ALGORITHM"
        return config.get<String>(key) orNotFound "JWT Auth Algorithm config is required"
    }

    @Provides
    @Singleton
    @Named("jwtAuthProvider")
    fun jwtAuthProvider(
        vertx: Vertx,
        @Named("jwtKeystorePath") jwtKeystorePath: String,
        @Named("jwtKeystorePassword") jwtKeystorePassword: String
    ): JWTAuth {
        val config = JWTAuthOptions().apply {
            keyStore = keyStoreOptionsOf(
                type = "pkcs12",
                path = jwtKeystorePath,
                password = jwtKeystorePassword
            )
        }
        return JWTAuth.create(vertx, config)
    }

    @Provides
    @Singleton
    @Named("maxLoginAttempt")
    fun maxLoginAttempt(config: JsonObject): Int {
        val key = "MAX_LOGIN_ATTEMPT"
        return config.get<Int>(key) orNotFound "Max login attempt config is required"
    }

    @Provides
    @Singleton
    @Named("autoUnlockedAccountAfter")
    fun autoUnlockedAccountAfter(config: JsonObject): Long {
        val key = "AUTO_UNLOCKED_ACCOUNT_AFTER"
        return config.get<Long>(key) orNotFound "Auto unlocked account after config is required"
    }
}
