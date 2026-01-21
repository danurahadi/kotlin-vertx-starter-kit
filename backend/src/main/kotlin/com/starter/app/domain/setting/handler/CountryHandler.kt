package com.starter.app.domain.setting.handler

import com.starter.app.domain.setting.db.repository.CountryRepository
import com.starter.app.domain.setting.settingLog
import id.yoframework.core.extension.logger.ERROR
import id.yoframework.core.extension.logger.INFO
import id.yoframework.core.extension.logger.logger
import id.yoframework.core.extension.system.executeTimeMillis
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler class for manage [Country] data
 *
 * @author Argi Danu Rahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class CountryHandler @Inject constructor(
    private val countryRepository: CountryRepository
) {
    private val log = logger<CountryHandler>()

    suspend fun getAllCountries(): JsonObject {
        
        /**
         * Core process
         *
         */
        val (data, coreTime) = executeTimeMillis {
            try {
                // create & execute query for get all countries
                val countries = countryRepository.findAll()
                    .sortedBy { it.name }

                // transform to DTO class
                countries.map { it.toCountryCompact() }
            } catch (ex: Exception) {
                log.settingLog(
                    ERROR("Get all countries from DB has been failed"),
                    "errors" to ex.message.toString()
                )
                throw ex
            }
        }

        log.settingLog(
            INFO("Get all countries from DB has been succeed"),
            "coreTime" to coreTime
        )
        /**
         * End of core process
         *
         */

        return json {
            obj(
                "data" to data,
                "message" to "All countries was successfully fetched."
            )
        }
    }
}
