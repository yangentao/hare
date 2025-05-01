package io.github.yangentao.hare.feature

import io.github.yangentao.anno.Length
import io.github.yangentao.anno.ModelField
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.utils.head
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.pool.ConnectionName
import io.github.yangentao.types.DateTime
import java.sql.Timestamp

object IPTrace {

    fun beforeRequest(context: HttpContext) {
        val date = DateTime()
        val m = IPRecord()
        m.uri = context.requestUri
        m.queryString = context.queryString?.head(4096)
        m.clientIP = context.removeIp
        m.remoteAddr = context.removeIp
        m.x_forwarded_for = context.requestHeader("X-Forwarded-For")
        m.x_real_ip = context.requestHeader("X-Real-IP")
        m.updateDate = date.formatDate()
        m.updateTime = date.formatTime()
        m.updateDateTime = date.timestamp
        m.insert()
    }
}

@ConnectionName("second")
class IPRecord : TableModel() {

    @ModelField(primaryKey = true, autoInc = 1)
    var id: Int by model

    @ModelField(index = true)
    var uri: String by model

    @ModelField
    @Length(max = 4096)
    var queryString: String? by model

    @ModelField(index = true)
    var clientIP: String? by model

    @ModelField
    var remoteAddr: String? by model

    @ModelField
    var x_forwarded_for: String? by model

    @ModelField
    var x_real_ip: String? by model

    @ModelField(index = true)
    var updateDate: String by model

    @ModelField(index = true)
    var updateTime: String by model

    @ModelField(index = true)
    var updateDateTime: Timestamp by model

    companion object : TableModelClass<IPRecord>()
}