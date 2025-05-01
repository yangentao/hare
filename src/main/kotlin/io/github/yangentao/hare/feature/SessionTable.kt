package io.github.yangentao.hare.feature

import io.github.yangentao.anno.Length
import io.github.yangentao.anno.ModelField
import io.github.yangentao.hare.utils.uuidString
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.clause.EQ
import io.github.yangentao.types.DAY_MILLS

class SessionTable : TableModel() {

    @ModelField(primaryKey = true)
    var accId: Long by model

    @ModelField(primaryKey = true)
    var platform: String by model

    // uuid 去掉 ‘-’, 固定32长度
    @Length(max = 256)
    @ModelField(index = true)
    var token: String by model

    /// account type
    @ModelField
    var type: String by model

    @ModelField
    var device: String? by model

    @ModelField
    @Length(max = 2048)
    var extra: String? by model

    @ModelField(index = true, defaultValue = "0")
    var timestamp: Long by model

    @ModelField(index = true)
    var expireTime: Long by model

    val expired: Boolean
        get() {
            if (expireTime != 0L) {
                return System.currentTimeMillis() >= expireTime
            }
            return false
        }

    companion object : TableModelClass<SessionTable>() {
        var durationTime: Long = 30L.DAY_MILLS

        fun remove(token: String) {
            SessionTable.delete(SessionTable::token EQ token)
        }

        fun check(token: String): SessionTable? {
            if (token.length != 32) return null
            return SessionTable.one(SessionTable::token EQ token)
        }

        fun make(id: Long, type: String, platform: String, device: String?): String {
            if (platform !in platformList) error("Invalid platform: $platform")

            val uuid = uuidString()
            val tm = System.currentTimeMillis()
            val t = SessionTable()
            t.accId = id
            t.platform = platform
            t.token = uuid
            t.type = type
            t.device = device
            t.timestamp = tm
            t.expireTime = tm + durationTime
            t.upsert()
            return uuid
        }

    }
}