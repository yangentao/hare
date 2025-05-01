package io.github.yangentao.hare.utils


import io.github.yangentao.kson.KsonObject

class JWT(private val pwd: String, private val header: String = """{"alg":"HS256","typ":"JWT"}""") {

    fun encode(body: String): String {
        val a = Encrypt.B64.encode(header)
        val b = Encrypt.B64.encode(body)
        val m = Encrypt.hmacSha256("$a.$b", pwd)
        return "$a.$b.$m"
    }

    fun decode(token: String): String? {
        val ls = token.split('.')
        if (ls.size != 3) return null
        val data = ls[1]
        val m = Encrypt.hmacSha256("${ls[0]}.$data", pwd)
        if (m == ls[2]) {
            return Encrypt.B64.decode(data)
        }
        return null
    }

    fun encodeIdPlat(ident: String, platform: String): String {
        return encode("""{"id":${ident.quoted},"plat":${platform.quoted}}""")
    }

    fun decodeIdPlat(token: String): Pair<String, String>? {
        val s = decode(token) ?: return null
        val yo = KsonObject(s)
        val id = yo.getString("id") ?: return null
        val plat = yo.getString("plat") ?: return null
        return id to plat
    }
}


