package com.starter.app.domain.setting.db.repository

import com.starter.app.domain.setting.db.model.TempFile
import com.starter.app.domain.setting.db.model.value.TempFileType
import id.yoframework.ebean.repository.Repository
import io.ebean.Database
import io.ebean.Query
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi.
 * @email danu.argi@gmail.com.
 */

@Singleton
class TempFileRepository @Inject constructor(ebeanServer: Database):
    Repository<TempFile, Long>(ebeanServer, TempFile::class) {

    suspend fun findByObjectKey(objectKey: String): Query<TempFile> {
        return query
            .select("*")
            .where()
            .eq("objectKey", objectKey)
            .query()
    }

    suspend fun findByType(type: TempFileType): Query<TempFile> {
        return query
            .select("*")
            .where()
            .eq("type", type)
            .query()
    }

    suspend fun findExpiredFiles(): Query<TempFile> {
        return query
            .select("*")
            .where()
            .le("expiredAt", LocalDateTime.now())
            .query()
    }

    suspend fun findByMediaLink(mediaLink: String): Query<TempFile> {
        return query
            .select("*")
            .where()
            .eq("mediaLink", mediaLink)
            .query()
    }

    suspend fun findByMediaLinks(mediaLinks: List<String>): Query<TempFile> {
        return query
            .select("*")
            .where()
            .isIn("mediaLink", mediaLinks)
            .query()
    }
}
