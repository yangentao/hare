package io.github.yangentao.hare

import io.github.yangentao.hare.utils.ieq

fun interface UriAlias {
    fun aliasOf(routePath: String): String?
}

class RenameAlias(val sourceUri: String, val targetUri: String) : UriAlias {
    override fun aliasOf(routePath: String): String? {
        return if (routePath ieq sourceUri) targetUri else null
    }

    override fun toString(): String {
        return "RenameAlias{ sourceUri= $sourceUri, targetUri= $targetUri}"
    }
}

// printX(RenamePathAlias("/static/", "/images/").tryAlias("/static/a.txt"))
// 完整匹配 matchEntire
class PrefixAlias(val sourcePath: String, val targetPath: String) : RegexAlias(Regex("""^$sourcePath(.*)$"""), """$targetPath{1}""") {
    override fun toString(): String {
        return "RenamePathAlias{ sourcePath= $sourcePath, targetPath= $targetPath }"
    }
}

//  /favicon.ico  =>  /static/favicon.ico
// RegexAlias("/(\w+\.\w{1,5})", "/static/{1}").match(xxxxx)
// RegexAlias("""^/(\w+\.\w+)/(\w+\.\w+)$""".toRegex(RegexOption.IGNORE_CASE), """/static/{2}/{1}""").match("/a.png/b.jpg")
// RegexAlias("/fav.icon".toRegex(RegexOption.IGNORE_CASE), "/static/fav.icon").match("/fav.icon")
// 完整匹配 matchEntire
open class RegexAlias(val pattern: Regex, val output: String) : UriAlias {
    private val argumentReg = """\{\s*(\d+)\s*}""".toRegex()
    override fun aliasOf(routePath: String): String? {
        try {
            val im = pattern.matchEntire(routePath) ?: return null
            return argumentReg.replace(output) { m ->
                val n = m.groups[1]?.value?.toIntOrNull() ?: error("match error: ")
                im.groups[n]?.value ?: error("match error: ")
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }

    override fun toString(): String {
        return "PatternAlisa{ pattern= $pattern, output= $output }"
    }
}