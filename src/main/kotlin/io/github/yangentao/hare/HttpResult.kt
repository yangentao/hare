@file:Suppress("unused")

package io.github.yangentao.hare

import io.github.yangentao.hare.utils.ieq
import io.github.yangentao.httpbasic.HttpStatus
import io.github.yangentao.kson.KsonValue
import io.github.yangentao.kson.ksonArray
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.toJson

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
        const val E_CODE: String = "E_CODE"
        const val E_MESSAGE: String = "E_MESSAGE"

        fun errorEx(exStatus: HttpStatus, data: String? = null, status: HttpStatus = HttpStatus.BAD_REQUEST): HttpResult {
            return HttpResult(data?.toByteArray(), mapOf(E_CODE to exStatus.code.toString(), E_MESSAGE to exStatus.reason), status = status)
        }

        fun error(statusCode: Int): HttpResult {
            return HttpResult(status = HttpStatus.valueOf(statusCode))
        }

        fun error(status: HttpStatus, data: ByteArray? = null): HttpResult {
            return HttpResult(content = data, status = status)
        }

        fun content(content: ByteArray, contentType: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return HttpResult(content, mapOf("Content-Type" to contentType, *headers), status)
        }

        fun binary(data: ByteArray, contentType: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return content(data, contentType, *headers, status = status)
        }

        fun json(kson: KsonValue, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return content(kson.toString().toByteArray(), CT.JSON_UTF8, *headers, status = status)
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

        fun model(model: TableModel, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return json(model.toJson(), headers = headers, status = status)
        }

        fun modelList(models: List<TableModel>, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            val arr = ksonArray(models) { it.toJson() }
            return json(arr, headers = headers, status = status)
        }
    }
}