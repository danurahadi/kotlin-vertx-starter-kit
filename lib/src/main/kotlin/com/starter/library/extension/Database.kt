package com.starter.library.extension

import io.ebean.Database
import io.ebean.annotation.Platform
import io.ebean.config.DatabaseConfig
import io.ebean.dbmigration.DbMigration

/**
 * Get inspired from yoframework by Deny Prasetyo
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 *
 */

fun Database.generateMigrationFile(
    platform: Platform,
    prefix: String
): String? {
    val ebean = this

    val serverConfig = DatabaseConfig().loadFromProperties()
    val dbMigration = DbMigration.create().apply {
        setServer(ebean)
        setServerConfig(serverConfig)
        addPlatform(platform, prefix)
        setApplyPrefix("V")
        setPathToResources("backend/src/main/resources")
        setStrictMode(true)
    }

    return dbMigration.generateMigration()
}
