package com.starter.library.extension

import kotlin.math.ceil

/**
 * [Documentation Here]
 *
 * @author Argi Danurahadi
 * @email danu.argi@gmail.com
 */

fun paginate(baseUrl: String, page: Int, limit: Int, totalData: Long): PaginationInfo {

    val perPage = if (totalData > 0) limit else 0
    val from = if (totalData > 0) ((page - 1) * perPage) + 1 else 0
    val to = if (totalData > 0
    ) {
        if (totalData < (page * perPage)
        ) {
            totalData.toInt()
        } else {
            page * perPage
        }
    } else {
        0
    }

    val currentPage = if (totalData > 0) page else 0
    val totalPage: Double = (totalData.toDouble() / perPage.toDouble())
    val lastPage = if (totalData > 0) ceil(totalPage).toInt() else 0

    val prevPageUrl = if (currentPage > 1) "${baseUrl}page=${currentPage - 1}&limit=$perPage" else null
    val nextPageUrl = if (currentPage < lastPage) "${baseUrl}page=${currentPage + 1}&limit=$perPage" else null

    return if (limit > 0
    ) {
        PaginationInfo(
            totalData = totalData,
            perPage = perPage,
            currentPage = currentPage,
            lastPage = lastPage,
            from = from.toLong(),
            to = to.toLong(),
            nextPageUrl = nextPageUrl,
            prevPageUrl = prevPageUrl
        )
    } else {
        PaginationInfo(
            totalData = totalData,
            perPage = totalData.toInt(),
            currentPage = 1,
            lastPage = 1,
            from = 1,
            to = totalData,
            nextPageUrl = null,
            prevPageUrl = null
        )
    }

}

data class PaginationInfo(
    val totalData: Long,
    val perPage: Int,
    val currentPage: Int,
    val lastPage: Int,
    val from: Long,
    val to: Long,
    val nextPageUrl: String?,
    val prevPageUrl: String?
)
