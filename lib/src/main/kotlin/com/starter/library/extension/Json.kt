package com.starter.library.extension

import io.vertx.core.json.JsonObject

fun JsonObject?.toMap(): Map<String, Any?> {
    return this?.map ?: emptyMap()
}
