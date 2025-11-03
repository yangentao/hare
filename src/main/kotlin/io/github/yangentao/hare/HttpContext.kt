@file:Suppress("unused")

package io.github.yangentao.hare

import io.github.yangentao.hare.utils.*
import io.github.yangentao.httpbasic.*
import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.kson.JsonResult
import io.github.yangentao.types.ICaseListMap
import io.github.yangentao.types.ICaseMap
import io.github.yangentao.types.firstValue
import io.github.yangentao.types.listValue
import java.io.File

//TODO 处理action直接返回错误码, 比如 404
abstract class HttpContext() {
    val requestHeaders: ICaseMap<String> = ICaseMap()
    val requestParameters: ICaseListMap<String> = ICaseListMap()
    val fileUploads: ArrayList<HttpFileParam> = ArrayList()

    //用于模块之间传递信息
    val attributes: HashMap<String, Any> = HashMap()
    private val cleanList: ArrayList<() -> Unit> = ArrayList()
    val timeMill: Long = System.currentTimeMillis()

    abstract val app: HttpApp
    abstract val requestUri: String
    abstract val queryString: String?
    abstract val commited: Boolean

    abstract val method: String
    abstract val removeAddress: String
    abstract val requestContent: ByteArray?
    fun requestHeader(name: String): String? = requestHeaders[name];
    abstract fun responseHeader(name: String, value: Any)
    abstract fun send(result: HttpResult)
    abstract fun sendError(status: HttpStatus)

    abstract fun sendFile(httpFile: HttpFile, attachment: Boolean)

    abstract val routePath: UriPath

    val removeIp: String by lazy {
        notBlankOf(removeAddress, requestHeader(HttpHeader.X_FORWARDED_FOR)?.split(",")?.firstOrNull()?.trim(), requestHeader(HttpHeader.X_REAL_IP)?.trim())
    }

    val dirUpload: File
        get() {
            return app.dirUpload
        }

    fun onAuthFailed(action: RouterAction) {
        sendResult(JsonFailed("未登录", code = 401))
    }

    fun errorClient(result: JsonResult? = null, message: String? = null, cause: Throwable? = null): Nothing {
        throw NetClientError(message, cause, result)
    }

    fun errorServer(message: String?, cause: Throwable? = null): Nothing {
        throw NetServerError(message, cause)
    }

    fun onClean(cb: () -> Unit) {
        cleanList.add(cb)
    }

    fun doClean() {
        cleanList.forEach { it() }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAttrOrPut(key: String, onMiss: (HttpContext) -> T): T {
        return attributes.getOrPut(key) { onMiss(this) } as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAttr(key: String): T? {
        return attributes[key] as? T
    }

    fun putAttr(key: String, value: Any) {
        attributes[key] = value
    }

    fun removeAttr(key: String) {
        attributes.remove(key)
    }

    fun fileUpload(name: String): HttpFileParam? {
        return this.fileUploads.firstOrNull { it.name ieq name }
    }

    fun hasParam(name: String): Boolean {
        return requestParameters.containsKey(name)
    }

    fun param(name: String): String? {
        return requestParameters.firstValue(name)
    }

    fun paramList(key: String): List<String> {
        return requestParameters.listValue(key)
    }

    //----------

    fun sendResult(result: JsonResult) {
        sendJson(result.toString())
    }

    fun sendBytes(data: ByteArray, contentType: String, status: HttpStatus = HttpStatus.OK) {
        send(HttpResult.content(data, contentType, status = status))
    }

    fun sendJson(json: String, status: HttpStatus = HttpStatus.OK) {
        sendBytes(json.toByteArray(), CT.JSON_UTF8, status)
    }

    fun sendHtml(html: String, status: HttpStatus = HttpStatus.OK) {
        sendBytes(html.toByteArray(), CT.HTML_UTF8, status)
    }

    fun sendXml(xml: String, status: HttpStatus = HttpStatus.OK) {
        sendBytes(xml.toByteArray(), CT.XML_UTF8, status)
    }

    fun sendText(text: String, status: HttpStatus = HttpStatus.OK) {
        sendBytes(text.toByteArray(), CT.PLAIN_UTF8, status)
    }

    fun sendBody(body: HttpBody, status: HttpStatus = HttpStatus.OK) {
        sendBytes(body.data, body.contentType, status)
    }

    fun sendError(status: HttpStatus, body: HttpBody) {
        sendBytes(body.data, body.contentType, status)
    }

    fun sendError404() {
        var html = HTML404
        val file = File(app.dirWeb, "404.html")
        if (file.exists() && file.canRead()) {
            quiet {
                html = file.readText()
            }
        }
        sendError(HttpStatus.NOT_FOUND, html.htmlBody)
    }

    fun sendError500(ex: Throwable) {
        val s = "500 Internal Server Error \n${ex.rootError.message} \n" + ex.stackTraceToString()
        sendError(HttpStatus.INTERNAL_SERVER_ERROR, s.textBody)
    }

    fun cors() {
        val origin = requestHeader(HttpHeader.ORIGIN)
        if (origin != null && this.method != HttpMethod.OPTIONS) {
            responseHeader(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
        }
    }

    fun requestDateHeader(name: String): Long {
        val s = requestHeader(name) ?: return -1
        return HttpHeader.parseHttpDate(s)
    }

    fun responseDateHeader(name: String, date: Long) {
        val s = HttpHeader.formatHttpDate(date)
        responseHeader(name, s)
    }

    // https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Reference/Headers/If-Match
    // If-Match: "bfc13a64729c4290ef5b2c2730249c88ca92d82d"
    // If-Match: W/"67ab43", "54ed21", "7892dd"
    // If-Match: *
    fun ifMatchEtag(etag: String): Boolean {
        val matchValue: String = requestHeader(HttpHeader.IF_MATCH)?.substringAfter('/')?.trim() ?: return true
        if (matchValue == "*") return true
        val e = etag.trim('"')
        val ls = matchValue.split(',').map { it.trim('"', ' ') }
        return ls.any { it == e }
    }

    // If-None-Match: "bfc13a64729c4290ef5b2c2730249c88ca92d82d"
    // If-None-Match: W/"67ab43", "54ed21", "7892dd"
    // If-None-Match: *
    fun ifNoneMatch(etag: String): Boolean {
        val matchValue: String = requestHeader(HttpHeader.IF_NONE_MATCH)?.substringAfter('/')?.trim() ?: return true
        if (matchValue == "*") return false
        val e = etag.trim('"')
        val ls = matchValue.split(',').map { it.trim('"', ' ') }
        return ls.all { it != e }
    }

    fun ifModifiedSince(lastModTime: Long): Boolean {
        val since: Long = requestDateHeader(HttpHeader.IF_MODIFIED_SINCE)
        return since == -1L || (since / 1000) < (lastModTime / 1000)
    }

    // If-Range: Wed, 21 Oct 2015 07:28:00 GMT
    // If-Range: "etag...."
    // https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Reference/Headers/If-Range
    fun ifRange(etag: String, time: Long): Boolean {
        val value: String = this.requestHeader(HttpHeader.IF_RANGE)?.trim(' ', '"') ?: return true
        if (etag.trim('"') == value) return true
        val tm = HttpHeader.parseHttpDate(value)
        return tm != -1L && tm / 1000 == time / 1000

    }

    // Range: bytes=200-1000, 2000-6576, 19000-
    // https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Reference/Headers/Range
    fun headerRanges(etag: String, time: Long, fileSize: Long): List<FileRange> {
        if (fileSize == 0L) {
            return emptyList()
        }
        if (!ifRange(etag, time)) return emptyList()

        val rangeHeader: String = requestHeader(HttpHeader.RANGE)?.substringAfter('=') ?: return emptyList()

        val ls = rangeHeader.split(',').map { it.trim() }
        val rList = ArrayList<FileRange>()
        for (p in ls) {
            val startEnd = p.split('-').mapNotNull { it.toLongOrNull() }
            if (startEnd.size == 2) {
                rList += FileRange(startEnd[0], startEnd[1])
            } else if (startEnd.size == 1) {
                rList += FileRange(startEnd[0], fileSize - 1)
            }
        }
        return rList.filter { it.size > 0 && it.start >= 0 && it.end < fileSize }
    }

    companion object {
        const val MIME_MULTIPART = "multipart/form-data"
        const val MIME_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded"
    }
}





