@file:OptIn(ExperimentalUuidApi::class)

package io.github.yangentao.hare.actions

import io.github.yangentao.hare.Action
import io.github.yangentao.hare.AfterAction
import io.github.yangentao.hare.BeforeAction
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.httpbasic.HttpFile
import io.github.yangentao.httpbasic.HttpFileParam
import io.github.yangentao.sql.clause.DESC
import io.github.yangentao.sql.clause.ORDER_BY
import io.github.yangentao.types.printX
import io.github.yangentao.xlog.logd
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Action(index = true)
class UploadPage(val context: HttpContext) {
    var idx = 0

    @BeforeAction
    fun beforeAll() {
        idx += 1
        logd("UploadActions.beforeAll : $idx")
    }

    @AfterAction
    fun afterAll() {
        idx += 1
        logd("UploadActions.afterAll : $idx")
    }

    @Action
    fun upload(file: HttpFileParam): String {
        idx += 1
        val u = Upload.fromHttpFile(file.httpFile, context.app.dirUpload)
        return u?.ident ?: "Failed"
    }

    @Action
    fun down1(ident: String): File? {
        val item = Upload.oneByKey(ident)
        return item?.localFile(context.app.dirUpload)
    }

    @Action
    fun down2(ident: String): Upload? {
        return Upload.oneByKey(ident)
    }

    @Action
    fun download(ident: String): HttpFile? {
        return Upload.oneByKey(ident)?.toHttpFile(context)
    }

    @Action
    fun dump(): String {
        val ls = Upload.list(null) { ORDER_BY(Upload::uploadTime.DESC) }
        for (item in ls) {
            printX(item.ident, " ", item.size, " ", item.rawName, " ", item.localName, " ", item.md5, item.mime)
        }
        return "OK"
    }
}

fun main() {
    val u = Uuid.random()
    printX(u.toString(), u.toHexString())

}
