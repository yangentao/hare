package io.github.yangentao.hare.feature

import io.github.yangentao.hare.log.logd
import io.github.yangentao.anno.ModelField
import io.github.yangentao.hare.ContextAttributeRequired
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.utils.ICaseMap
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.pool.ConnectionName
import io.github.yangentao.sql.update
import io.github.yangentao.types.format

private var HttpContext.requestStartTime: Long by ContextAttributeRequired(0L)

object ReqTimeSlice {
    val map = ICaseMap<Int>()

    fun beforeRequest(context: HttpContext) {
        if (map.isEmpty()) {
            ReqTime.count("*")
        }
        val uri = context.requestUri
        val n = map.getOrPut(uri) { 0 }
        map[uri] = n + 1
        //该uri的第一次请求, 不计时.  会有一些初始化的工作
        if (n == 0) return
        context.requestStartTime = System.currentTimeMillis()
    }

    fun afterRequest(context: HttpContext) {
        val start = context.requestStartTime
        if (start == 0L) return
        val uri = context.requestUri
        val delta = System.currentTimeMillis() - start
        logd("请求", uri, " 用时 ", (delta / 1000.0).format("0.000"), "秒")

        val old = ReqTime.oneByKey(uri)
        if (old != null) {
            old.update {
                it.times += 1
                it.totalTime += delta
                it.avgTime = it.totalTime / it.times
                it.maxTime = java.lang.Long.max(it.maxTime, delta)
            }
        } else {
            val a = ReqTime()
            a.uri = uri
            a.times = 1
            a.totalTime = delta
            a.maxTime = delta
            a.avgTime = delta
            a.insert()
        }
    }
}

@ConnectionName("second")
class ReqTime : TableModel() {
    @ModelField(primaryKey = true)
    var uri: String by model

    @ModelField
    var times: Long by model

    @ModelField
    var totalTime: Long by model

    @ModelField
    var avgTime: Long by model

    @ModelField
    var maxTime: Long by model

    companion object : TableModelClass<ReqTime>()
}