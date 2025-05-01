package io.github.yangentao.hare.actions

import io.github.yangentao.hare.Action
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.UriMatch
import io.github.yangentao.hare.utils.ieq
import io.github.yangentao.httpbasic.HttpFile
import java.io.File

//val st = StaticFileActions(Path.of("/favicon.ico"), File("/static/favicon.ico"))
//router.addMatch(st, st::service)
//单一文件下载服务
class StaticFileService(val path: String, val httpFile: HttpFile) : UriMatch {
    val file: File get() = httpFile.file

    //检查, 安全,  . 和 ..
    @Action
    fun service(context: HttpContext): HttpFile? {
        if (!file.exists() || !file.isFile || file.isHidden) {
            return null
        } else {
            return httpFile
        }
    }

    override fun match(targetPath: String): Boolean {
        return path ieq targetPath
    }
}

