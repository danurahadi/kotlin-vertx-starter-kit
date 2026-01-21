package com.starter.app.integration.sms

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import id.yoframework.core.exception.DataInconsistentException
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com
 */

@Singleton
class SmsService @Inject constructor(
    @param:Named("disableSms") private val disableSms: Boolean,
    @param:Named("twilioAccountSid") private val twilioAccountSid: String,
    @param:Named("twilioAuthToken") private val twilioAuthToken: String,
    @param:Named("twilioRegion") private val twilioRegion: String,
    @param:Named("twilioEdge") private val twilioEdge: String,
    @param:Named("twilioSenderPhoneNumber") private val twilioSenderPhoneNumber: String
) {
    private val log = logger(SmsService::class)

    fun sendSMS(recipient: String, textMessage: String): Message {
        if (disableSms) {
            throw DataInconsistentException("SMS service is disabled. Please enable it first.")
        }

        // initialize Twilio Client
        Twilio.init(twilioAccountSid, twilioAuthToken)
        Twilio.setRegion(twilioRegion)
        Twilio.setEdge(twilioEdge)

        // send messages via Twilio
        val response = Message.creator(
            twilioAccountSid,
            PhoneNumber(recipient),
            PhoneNumber(twilioSenderPhoneNumber),
            textMessage
        ).create()

        log.smsLog(
            INFO("SMS has been sent to $recipient."),
            "response" to response.toString()
        )

        return response
    }
}
