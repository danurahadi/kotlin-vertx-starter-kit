package com.starter.app.domain.auth.plain

import id.yoframework.core.model.Model
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO class for hold and validate user's login data
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

class Login() : Model {
    constructor(
        identity: String,
        password: String,
        staySignedIn: Boolean
    ) : this() {
        this.identity = identity
        this.password = password
        this.staySignedIn = staySignedIn
    }

    @NotBlank(message = "Identity could not be blank.")
    @Size(max = 100, message = "Identity could not be more than 100 chars.")
    lateinit var identity: String

    @NotBlank(message = "Password could not be blank.")
    @Size(max = 100, message = "Password could not be more than 100 chars.")
    lateinit var password: String

    var staySignedIn: Boolean = false
}
