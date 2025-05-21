package io.github.yangentao.hare.utils

import io.github.yangentao.anno.Name
import io.github.yangentao.anno.userName
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.HttpParameter
import io.github.yangentao.hare.HttpParameterOr
import io.github.yangentao.hare.OnHttpContext
import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.kson.JsonResult
import io.github.yangentao.tag.TagContext
import kotlin.reflect.KProperty

val NotLogin: JsonResult get() = JsonFailed("未登录", 401)
val NoPermission: JsonResult get() = JsonFailed("无权限", 403)
val BadValue: JsonResult get() = JsonFailed("无效数据")

inline fun <reified T : Any> T?.ifNotNull(block: (T) -> Unit): T? {
    if (this != null) block(this)
    return this
}

@Name("limit")
val OnHttpContext.limitValue: Int? by HttpParameterOr

@Name("offset")
val OnHttpContext.offsetValue: Int by HttpParameter(0)

//inline fun <reified T : Any> T?.ifNull(block: () -> Unit): T? {
//    if (this == null) block(this)
//    return this
//}
val HttpContext.tagContext: TagContext
    get() = object : TagContext {
        override fun paramValue(key: String): String? {
            return this@tagContext.param(key)
        }
    }
