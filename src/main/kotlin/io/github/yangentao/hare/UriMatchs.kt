package io.github.yangentao.hare


import io.github.yangentao.hare.utils.PatternText
import io.github.yangentao.hare.utils.UriPath
import io.github.yangentao.hare.utils.ieq

fun interface UriMatch {
    fun match(targetPath: String): Boolean
}

class EqualMatch(val path: String) : UriMatch {
    override fun match(targetPath: String): Boolean {
        return targetPath ieq path
    }
}

// PrefixMatch("/static")
// lowercase
class PrefixUriMatch(val prefixUri: String) : UriMatch {
    override fun match(targetPath: String): Boolean {
        if (prefixUri.isEmpty() || prefixUri == "/") return true
        return UriPath(targetPath).hasPrefix(prefixUri)
    }

    override fun toString(): String {
        return "PrefixUriMatch{ prefixUri: $prefixUri}"
    }
}

//RegexMatch("")
class RegexUriMatch(val regex: Regex) : UriMatch {
    constructor(rule: String) : this(rule.toRegex(RegexOption.IGNORE_CASE))

    override fun match(targetPath: String): Boolean {
        return targetPath.matches(regex)
    }

    override fun toString(): String {
        return "RegexUriMatch{ regex: ${regex.pattern}}"
    }

}

// pattern: "/delete/{ident}/{name}"
class PatternUriMatch(val pattern: String, val entire: Boolean = true) : UriMatch {
    private val pt = PatternText(pattern)

    //  "/delete/123/true"
    override fun match(targetPath: String): Boolean {
        if (entire) {
            pt.tryMatchEntire(targetPath) ?: return false
        } else {
            pt.tryMatchAt(targetPath, 0) ?: return false
        }
        return true
    }

    fun matchResult(targetPath: String): Map<String, String>? {
        return if (entire) {
            pt.tryMatchEntire(targetPath)
        } else {
            pt.tryMatchAt(targetPath, 0)
        }
    }

    override fun toString(): String {
        return "PatternUriMatch{ pattern = ${PathType.pattern}, entire: $entire}"
    }
}
