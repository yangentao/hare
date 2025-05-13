package io.github.yangentao.hare

import io.github.yangentao.hare.feature.accountID
import io.github.yangentao.types.DateTime

open class HttpController(override val context: HttpContext) : OnHttpContext {
    val timeNow: Long by lazy { System.currentTimeMillis() }
    val dateTime: DateTime by lazy { DateTime(timeNow) }
    val accountID:Long get() = context.accountID ?: 0L
}