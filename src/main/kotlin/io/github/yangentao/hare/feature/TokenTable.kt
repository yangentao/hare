package io.github.yangentao.hare.feature

import io.github.yangentao.anno.Length
import io.github.yangentao.anno.ModelField
import io.github.yangentao.hare.utils.JWT
import io.github.yangentao.kson.KsonObject
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.clause.EQ
import io.github.yangentao.types.TimeValue
import io.github.yangentao.types.timeDays

class TokenTable : TableModel() {

    @ModelField(primaryKey = true)
    var id: Long by model

    @ModelField(primaryKey = true)
    var type: String by model

    /// device_id
    @ModelField(primaryKey = true)
    var platform: String by model

    @ModelField(index = true, defaultValue = "0")
    var timestamp: Long by model

    @ModelField(index = true)
    var expireTime: Long by model

    @Length(max = 1024)
    @ModelField(index = true)
    var token: String by model

    val expired: Boolean
        get() {
            if (expireTime != 0L) {
                return System.currentTimeMillis() >= expireTime
            }
            return false
        }

    companion object : TableModelClass<TokenTable>() {
        var pwd: String = "1234567890"

        fun removeToken(token: String?) {
            if (token != null) delete(TokenTable::token EQ token)
        }

        fun removeToken(id: Int, type: String) {
            delete(TokenTable::id EQ id, TokenTable::type EQ type)
        }

        fun checkToken(token: String): TokenTable? {
            val data = JWT(pwd).decode(token) ?: return null
            val body = TokenBody(KsonObject(data))
            if (body.expired) {
                removeToken(token)
                return null
            }
            return one(TokenTable::token EQ token)
        }

        fun makeToken(ident: Long, type: String, platform: String, durationTime: TimeValue = 30.timeDays): String {
            val tm = System.currentTimeMillis()
            val a = TokenBody()
            a.id = ident
            a.type = type
            a.platform = platform
            a.timestamp = tm
            a.expireTime = tm + durationTime.toMilliSeconds.value
            return makeToken(a)
        }

        fun makeToken(body: TokenBody): String {
            val token = JWT(pwd).encode(body.toString())
            val ut = TokenTable()
            ut.id = body.id
            ut.type = body.type
            ut.expireTime = body.expireTime
            ut.platform = body.platform
            ut.timestamp = body.timestamp
            ut.token = token
            ut.upsert()
            return token
        }
    }
}

class TokenBody(private val yo: KsonObject = KsonObject()) {
    var id: Long by yo
    var type: String by yo
    var expireTime: Long by yo
    var platform: String by yo
    var timestamp: Long by yo

    val expired: Boolean
        get() {
            if (expireTime != 0L) {
                return System.currentTimeMillis() >= expireTime
            }
            return false
        }

    override fun toString(): String {
        return yo.toString()
    }

    companion object {

        @Deprecated("Use TokenTable.makeToken instead")
        fun make(ident: Long, type: String, platform: String, durationTime: TimeValue = 30.timeDays): TokenBody {
            val tm = System.currentTimeMillis()
            val a = TokenBody()
            a.id = ident
            a.type = type
            a.platform = platform
            a.timestamp = tm
            a.expireTime = tm + durationTime.toMilliSeconds.value
            return a
        }
    }

}
