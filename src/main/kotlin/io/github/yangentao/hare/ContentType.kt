package io.github.yangentao.hare

import io.github.yangentao.hare.utils.ieq
import java.nio.charset.Charset

//https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Content-Type
//ContentType("text/html","utf-8")
class ContentType(mime: String, charsetName: String) {
    val mime: String = mime.lowercase()
    val charsetName: String = charsetName.lowercase()
    //val boundary:String

    val contentType: String get() = if (charsetName.isEmpty()) mime else "$mime; charset=$charsetName"

    val charset: Charset get() = if (charsetName.isEmpty()) Charsets.UTF_8 else fromName(charsetName, Charsets.UTF_8) //ISO_8859_1

    @Suppress("SameParameterValue")
    private fun fromName(name: String, fallback: Charset): Charset {
        try {
            return Charset.forName(name)
        } catch (ex: Throwable) {
            return fallback
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ContentType) return false
        return mime == other.mime && charsetName == other.charsetName
    }

    override fun hashCode(): Int {
        return mime.hashCode() + charsetName.hashCode()
    }

    override fun toString(): String {
        return contentType
    }

    companion object {
        val textUTF8 = ContentType("text/plain", "utf-8")
        val htmlUTF8 = ContentType("text/html", "utf-8")
        val jsonUTF8 = ContentType("application/json", "utf-8")
        val octetStream = ContentType("application/octet-stream", "")

        fun parse(contentType: String): ContentType? {
            val ls = contentType.split(';', '=')
            if (ls.isEmpty()) return null
            if (ls.size == 1) return ContentType(ls.first().trim(), "")
            if (ls.size == 3 && ls[1].trim() ieq "charset") return ContentType(ls.first().trim(), ls[2].trim())
            return null
        }
    }
}

object CT {
    const val HTML_UTF8 = "text/html; charset=utf-8"
    const val PLAIN_UTF8 = "text/plain; charset=utf-8"
    const val JSON_UTF8 = "application/json; charset=utf-8"
    const val XML_UTF8 = "application/xml; charset=utf-8"
    const val OCTET_STREAM = "application/octet-stream"

    fun build(mime: String, charset: Charset?): String {
        if (charset == null) return mime
        return mime + "; charset=" + charset.name()
    }
}

