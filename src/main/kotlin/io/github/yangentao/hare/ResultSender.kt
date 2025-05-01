package io.github.yangentao.hare

import io.github.yangentao.hare.actions.Upload
import io.github.yangentao.httpbasic.HttpFile
import io.github.yangentao.httpbasic.HttpStatus
import io.github.yangentao.httpbasic.Mimes
import io.github.yangentao.httpbasic.httpFile
import io.github.yangentao.kson.JsonResult
import io.github.yangentao.kson.KsonArray
import io.github.yangentao.kson.KsonObject
import io.github.yangentao.tag.html.HtmlTag
import io.github.yangentao.tag.xml.XmlTag
import java.io.File
import java.nio.charset.Charset

fun interface ResultSender {
    fun sendResult(context: HttpContext, action: RouterAction, result: Any): Boolean
}

class DefaultResultSender : ResultSender {
    override fun sendResult(context: HttpContext, action: RouterAction, result: Any): Boolean {

        when (result) {
            is JsonResult -> context.sendJson(result.toString())
            is HttpResult -> context.send(result)
            is HttpBody -> context.sendBody(result)
            is HttpStatus-> context.sendError(result)
            is String -> {
                val cs: Charset = action.charset
                context.sendBytes(result.toByteArray(cs), CT.build(action.mime ?: Mimes.PLAIN, cs))
            }

            is KsonObject, is KsonArray -> context.sendJson(result.toString())

            is ByteArray -> context.sendBytes(result, action.mime ?: Mimes.OCTET_STREAM)
            is File -> context.sendFile(result.httpFile(), attachment = false)
            is HttpFile -> context.sendFile(result, attachment = false)
            is Upload -> context.sendFile(result.toHttpFile(context), attachment = false)
            is HtmlTag -> context.sendHtml(result.toString())
            is XmlTag -> context.sendXml(result.toString())
            else -> return false
        }
        return true
    }
}