package io.github.yangentao.hare.utils

import io.github.yangentao.anno.Name
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.HttpParameter
import io.github.yangentao.hare.HttpParameterOr
import io.github.yangentao.hare.OnHttpContext
import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.kson.JsonResult
import io.github.yangentao.tag.TagContext
import java.util.*

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
    get() = HttpTagContext(this)

class HttpTagContext(val httpContext: HttpContext) : TagContext {
    override fun paramValue(key: String): String? {
        return httpContext.param(key)
    }

}

//val s  = "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, zh-CN;q=0.9, *;q=0.5"
//val ls = parseAcceptLanguage(s)
@Suppress("DEPRECATION")
fun parseAcceptLanguage(al: String): List<Locale> {
    val ls = al.split(',').map { it.trim() }
    val plist: ArrayList<Pair<Locale, Double>> = ArrayList<Pair<Locale, Double>>()
    for (item in ls) {
        val ql = item.split(';')
        val l = ql.firstOrNull()?.trim() ?: continue
        if (l.isEmpty() || l == "*") continue
        val q: Double = ql.secondOrNull()?.substringAfter('=')?.toDoubleOrNull() ?: 1.0
        val loc = Locale(l.substringBefore('-'), l.substringAfter('-', "").substringBefore('-'))
        plist.add(loc to q)
    }
    plist.sortByDescending { it.second }
    return plist.map { it.first }
}
