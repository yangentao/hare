package io.github.yangentao.hare

open class HttpBody(val data: ByteArray, val contentType: String) {
    companion object {
        fun text(data: String) = HttpBody(data.toByteArray(), CT.PLAIN_UTF8)
        fun html(data: String) = HttpBody(data.toByteArray(), CT.HTML_UTF8)
        fun json(data: String) = HttpBody(data.toByteArray(), CT.JSON_UTF8)
        fun xml(data: String) = HttpBody(data.toByteArray(), CT.XML_UTF8)
        fun binary(data: ByteArray, mime: String? = null) = HttpBody(data, mime ?: CT.OCTET_STREAM)
    }
}

class TextHttpBody(data: String) : HttpBody(data.toByteArray(), CT.PLAIN_UTF8)
class HtmlHttpBody(data: String) : HttpBody(data.toByteArray(), CT.HTML_UTF8)
class JsonHttpBody(data: String) : HttpBody(data.toByteArray(), CT.JSON_UTF8)
class XmlHttpBody(data: String) : HttpBody(data.toByteArray(), CT.XML_UTF8)
class BinaryHttpBody(data: ByteArray) : HttpBody(data, CT.OCTET_STREAM)

val String.textBody: HttpBody get() = TextHttpBody(this)
val String.htmlBody: HttpBody get() = HtmlHttpBody(this)
val String.jsonBody: HttpBody get() = JsonHttpBody(this)
val String.xmlBody: HttpBody get() = XmlHttpBody(this)