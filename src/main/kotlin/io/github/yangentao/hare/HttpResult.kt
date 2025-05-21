package io.github.yangentao.hare

import io.github.yangentao.hare.utils.ieq
import io.github.yangentao.httpbasic.HttpStatus

class HttpResult(val content: ByteArray? = null, val headers: Map<String, String> = emptyMap(), val status: HttpStatus = HttpStatus.OK) {

    val contentLength: Int get() = content?.size ?: 0
    val isEmptyContent: Boolean get() = contentLength == 0

    fun containsHeader(header: String): Boolean {
        for (k in headers.keys) {
            if (k ieq header) return true
        }
        return false
    }

    companion object {
        fun error(statusCode: Int): HttpResult {
            return HttpResult(status = HttpStatus.valueOf(statusCode))
        }

        fun error(status: HttpStatus): HttpResult {
            return HttpResult(status = status)
        }

        fun content(content: ByteArray, contentType: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return HttpResult(content, mapOf("Content-Type" to contentType, *headers), status)
        }

        fun json(json: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return content(json.toByteArray(), CT.JSON_UTF8, *headers, status = status)
        }

        fun xml(xml: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return content(xml.toByteArray(), CT.XML_UTF8, *headers, status = status)
        }

        fun text(text: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return content(text.toByteArray(), CT.PLAIN_UTF8, *headers, status = status)
        }

        fun html(html: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return content(html.toByteArray(), CT.HTML_UTF8, *headers, status = status)
        }
    }
}