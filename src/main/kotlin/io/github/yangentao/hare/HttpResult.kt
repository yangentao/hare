@file:Suppress("unused")

package io.github.yangentao.hare

import io.github.yangentao.hare.utils.ieq
import io.github.yangentao.httpbasic.HttpHeader
import io.github.yangentao.httpbasic.HttpStatus
import io.github.yangentao.kson.KsonValue
import io.github.yangentao.kson.ksonArray
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.toJson
import io.github.yangentao.types.ICaseMap

class HttpResult(val content: ByteArray? = null, val status: HttpStatus = HttpStatus.OK, contentType: String? = null) {
    val headers: ICaseMap<String> = ICaseMap()
    var contentType: String?
        get() = headers[HttpHeader.CONTENT_TYPE]
        set(value) {
            if (value.isNullOrEmpty()) headers.remove(HttpHeader.CONTENT_TYPE) else headers[HttpHeader.CONTENT_TYPE] = value
        }

    init {
        headers[HttpHeader.CONTENT_LENGTH] = (content?.size ?: 0).toString()
        if (contentType != null && contentType.isNotEmpty()) this.contentType = contentType
    }

    val contentLength: Int get() = content?.size ?: 0
    val isEmptyContent: Boolean get() = contentLength == 0
    val success: Boolean get() = status.success && (errorCode == null || errorCode == 0)

    var errorCode: Int?
        get() = headers[E_CODE]?.toInt()
        set(value) {
            if (value == null) headers.remove(E_CODE) else headers[E_CODE] = value.toString()
        }
    var errorMessage: String?
        get() = headers[E_MESSAGE]
        set(value) {
            if (value == null || value.isEmpty()) headers.remove(E_MESSAGE) else headers[E_MESSAGE] = value
        }

    fun containsHeader(header: String): Boolean {
        for (k in headers.keys) {
            if (k ieq header) return true
        }
        return false
    }

    companion object {
        const val E_CODE: String = "E_CODE"
        const val E_MESSAGE: String = "E_MESSAGE"

        fun errorX(codeMessage: CodeMessage, status: HttpStatus = statusByECode(codeMessage.code), data: Any? = null): HttpResult {
            return errorX(codeMessage.message, codeMessage.code, status, data)
        }

        fun errorX(message: String, code: Int = -1, status: HttpStatus = statusByECode(code), data: Any? = null): HttpResult {
            val bytes: ByteArray? = when (data) {
                null, Unit -> null
                is String -> data.toByteArray()
                is Number, is Boolean -> data.toString().toByteArray()
                is Throwable -> (data.toString() + "\n" + data.stackTraceToString()).toByteArray()
                is KsonValue -> data.toString().toByteArray()
                is TableModel -> data.toJson().toString().toByteArray()
                else -> data.toString().toByteArray()
            }
            return HttpResult(content = bytes, contentType = CT.PLAIN_UTF8, status = status).also {
                it.errorCode = code
                it.errorMessage = message.lines().joinToString(", ")
            }
        }

        fun error(status: HttpStatus, data: ByteArray? = null, contentType: String? = CT.PLAIN_UTF8): HttpResult {
            return HttpResult(content = data, contentType = contentType, status = status)
        }

        fun content(content: ByteArray, contentType: String, vararg headers: Pair<String, String>, status: HttpStatus = HttpStatus.OK): HttpResult {
            return HttpResult(content = content, status = status, contentType = contentType).also { it.headers.putAll(headers) }
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
    return HttpResult.errorX(message = message, code = code, status = this, data = data)
}

class CodeMessage(val code: Int, val message: String) {

    companion object {
        val OK: CodeMessage = CodeMessage(0, "OK")
    }
}

class StatusException(val result: HttpResult) : Exception() {
    override fun toString(): String {
        return "StatusException: ${result.status}, ${result.errorCode} ${result.errorMessage}"
    }

    companion object {
        var defaultStatus: HttpStatus = HttpStatus.OK
        var autoStatus: Boolean = false
    }
}

fun errorStatus(status: HttpStatus, data: ByteArray? = null, contentType: String? = CT.PLAIN_UTF8): Nothing {
    throw StatusException(HttpResult.error(status = status, data = data, contentType = contentType))
}

fun errorStatus(message: String, code: Int = -1, status: HttpStatus = StatusException.defaultStatus, data: Any? = null): Nothing {
    throw StatusException(HttpResult.errorX(message = message, code = code, status = status, data = data))
}

fun errorStatus(codeMessage: CodeMessage, status: HttpStatus = StatusException.defaultStatus, data: Any? = null): Nothing {
    throw StatusException(HttpResult.errorX(message = codeMessage.message, code = codeMessage.code, status = status, data = data))
}

fun statusByECode(code: Int): HttpStatus {
    if (StatusException.autoStatus && code in 400..599) return HttpStatus.valueOf(code)
    return StatusException.defaultStatus
}