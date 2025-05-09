package io.github.yangentao.hare.actions

import io.github.yangentao.hare.Action
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.PrefixUriMatch
import io.github.yangentao.hare.log.logd
import io.github.yangentao.httpbasic.HttpStatus
import io.github.yangentao.httpbasic.httpFile
import java.io.File

//val st = StaticDirectoryActions(Path.of("/static"), File("static"))
//router.addMatch(st, st::service)
//目录服务, 下载, 列表
class StaticDirectoryService(prefix: String, val localDir: File, val allowListDir: Boolean = true) {
    val prefixUri: String = prefix.trim('/')
    val matcher = PrefixUriMatch(prefixUri)

    //检查, 安全,  . 和 ..
    @Action
    fun service(context: HttpContext): Any? {
        logd("static service: ", context.requestUri)
        var childPath = context.routePath.trimStartPath(prefixUri) ?: return HttpStatus.NOT_FOUND
        childPath = childPath.trim('/')

        if (".." in childPath) return HttpStatus.NOT_FOUND
        val file = File(localDir, childPath)
        logd("local path: ", file.canonicalPath)
        if (!file.exists() || file.isHidden) return HttpStatus.NOT_FOUND
        if (file.isFile && file.canRead()) {
            return file.httpFile()
        }
        if (!allowListDir) return HttpStatus.NOT_FOUND
        if (file.isDirectory) {
            val ls = file.listFiles { f ->
                !f.isHidden
            } ?: return HttpStatus.NOT_FOUND
            if (ls.isEmpty()) {
                return ""
            } else {
                return ls.joinToString("\n") { if (it.isDirectory) it.name + "/" else it.name }
            }
        }
        return HttpStatus.NOT_FOUND
    }

//    override fun match(targetPath: String): Boolean {
//        if (prefixUri.isEmpty()) return true
//        logd("static match: ", targetPath, "  -> ", prefixUri)
//        return UriPath(targetPath).hasPrefix(prefixUri)
//    }
}