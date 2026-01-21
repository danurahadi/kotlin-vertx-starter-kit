package com.starter.app.integration.mailer

import com.starter.library.module.EnvModule
import dagger.Module
import dagger.Provides
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(includes = [EnvModule::class])
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class MailerModule {

    @Provides
    @Singleton
    @Named("disableMailer")
    fun disableMailer(config: JsonObject): Boolean {
        val key = "DISABLE_MAILER"
        return config.getBoolean(key, false)
    }

    @Provides
    @Singleton
    @Named("mailerSender")
    fun mailerSender(config: JsonObject): String {
        val key = "MAILER_SENDER"
        return config.getString(key, "Jogja Node <no-reply@jogjanode.com>")
    }

    @Provides
    @Singleton
    @Named("mailerAccount")
    fun mailerAccount(config: JsonObject): Pair<String, String> {
        val userNameKey = "MAILER_USERNAME"
        val username = config.getString(userNameKey, "api")
        val passwordKey = "MAILER_PASSWORD"
        val password = config.getString(passwordKey, "key-437e854d374d420e00efa7fe1243b50c")
        return username to password
    }

    @Provides
    @Singleton
    @Named("mailerUrl")
    fun mailerUrl(config: JsonObject): String {
        val key = "MAILER_URL"
        return config.getString(key, "https://api.mailgun.net/v3/mg.jogjanode.com/messages")
    }

    @Provides
    @Singleton
    fun mailerService(
        @Named("mailerSender") sender: String,
        @Named("mailerUrl") mailerUrl: String,
        @Named("mailerAccount") account: Pair<String, String>,
        @Named("disableMailer") disableMailer: Boolean
    ): MailerService {
        return MailerService(sender, mailerUrl, account, disableMailer)
    }
}
