package com.starter.app.app.verticle

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
@ObsoleteCoroutinesApi
@DelicateCoroutinesApi
class WorkerVerticle @Inject constructor(): AbstractVerticle() {
    override fun start(startPromise: Promise<Void>) {
        startPromise.complete()
    }
}
