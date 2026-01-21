package com.starter.app.domain.setting.db.model

import com.starter.app.domain.setting.db.model.value.TempFileType
import id.yoframework.core.model.Model
import io.ebean.annotation.DbDefault
import io.ebean.annotation.WhenCreated
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Entity
@Table(name = "temp_files")
class TempFile() : Model {
    constructor(
        id: Long? = null,
        objectKey: String,
        mediaLink: String,
        type: TempFileType,
        expiredAt: LocalDateTime = LocalDateTime.now().plusHours(24),
        createdAt: LocalDateTime = LocalDateTime.now()
    ) : this() {
        this.id = id
        this.objectKey = objectKey
        this.mediaLink = mediaLink
        this.type = type
        this.expiredAt = expiredAt
        this.createdAt = createdAt
    }

    @Id
    var id: Long? = 0

    lateinit var objectKey: String
    lateinit var mediaLink: String

    lateinit var type: TempFileType
    lateinit var expiredAt: LocalDateTime

    @WhenCreated
    @DbDefault(value = "2022-12-12 00:00:00")
    lateinit var createdAt: LocalDateTime
}
