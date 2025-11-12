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
    val success: Boolean get() = status.success && errorCode == 0

    val errorCode: Int get() = headers[E_CODE]?.toInt() ?: 0
    val errorMessage: String? get() = headers[E_MESSAGE]

    fun containsHeader(header: String): Boolean {
        for (k in headers.keys) {
            if (k ieq header) return true
        }
        return false
    }

    companion object {
        const val E_CODE: String = "E_CODE"
        const val E_MESSAGE: String = "E_MESSAGE"

        fun errorX(codeMessage: CodeMessage, data: Any? = null, status: HttpStatus = HttpStatus.BAD_REQUEST): HttpResult {
            return errorX(codeMessage.message, codeMessage.code, data, status)
        }

        fun errorX(message: String, code: Int = -1, data: Any? = null, status: HttpStatus = HttpStatus.BAD_REQUEST): HttpResult {
            val bytes: ByteArray? = when (data) {
                null, Unit -> null
                is String -> data.toByteArray()
                is Number, is Boolean -> data.toString().toByteArray()
                is CodeException -> data.toString().toByteArray()
                is Throwable -> {
                    (data.toString() + "\n" + data.stackTraceToString()).toByteArray()
                }

                else -> data.toString().toByteArray()
            }
            val msg = message.lines().joinToString(", ")
            return HttpResult(content = bytes, headers = mapOf(E_CODE to code.toString(), E_MESSAGE to msg), status = status)
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

val HttpStatus.success get() = this.code in 200..299

fun HttpStatus.error(message: String, code: Int = -1, data: Any? = null): HttpResult {
    assert(!this.success)
    return HttpResult.errorX(message = message, code = code, data = data, status = this)
}

class CodeMessage(val code: Int, val message: String) {

    companion object {
        val OK: CodeMessage = CodeMessage(0, "OK")
    }
}