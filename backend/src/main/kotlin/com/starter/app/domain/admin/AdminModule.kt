package com.starter.app.domain.admin

import com.starter.app.domain.user.UserModule
import com.starter.app.integration.fileupload.aws.AwsS3Module
import com.starter.app.integration.mailer.MailerModule
import dagger.Module
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi

/**
 * Class that represent Admin Module and will provide some value, such as from config file.
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Module(
    includes = [
        MailerModule::class,
        AwsS3Module::class,
        UserModule::class
    ]
)
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class AdminModule
