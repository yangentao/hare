package io.github.yangentao.hare.utils

import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.kson.JsonResult

val NotLogin: JsonResult get() = JsonFailed("未登录", 401)
val NoPermission: JsonResult get() = JsonFailed("无权限", 403)
val BadValue: JsonResult get() = JsonFailed("无效数据")

object StateVal {
    const val NORMAL: Int = 0
    const val DISABLED: Int = 1
    const val DELETED: Int = 2
}