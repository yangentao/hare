package io.github.yangentao.hare.utils

import java.util.LinkedHashMap

// pattern: "HELLO {ident} {name}"
class PatternText(val pattern: String) {
    private val paramMatchResult: List<MatchResult> = argReg.findAll(pattern).toList()
    private val patternReg: Regex = argReg.replace(pattern, """\\s*(\\w+)\\s*""").toRegexIC
    val paramNames: List<String> = paramMatchResult.mapNotNull { it.groups[1]?.value }

    //  "HELLO 123 entao"
    //  {ident=123, name=entao}
    // 返回null: 不匹配; 可能返回空map
    fun tryMatchEntire(text: String): Map<String, String>? {
        val matchResult: MatchResult = patternReg.matchEntire(text) ?: return null
        val matchList: List<MatchGroup?> = matchResult.groups.toList()
        val resultMap = LinkedHashMap<String, String>()
        for (i in paramMatchResult.indices) {
            val k = paramMatchResult[i].groups[1]?.value ?: continue
            val v = matchList[i + 1]?.value
            resultMap[k] = v ?: ""
        }
        return resultMap
    }

    fun tryMatchAt(text: String, index: Int): Map<String, String>? {
        val matchResult: MatchResult = patternReg.matchAt(text, index) ?: return null
        val matchList: List<MatchGroup?> = matchResult.groups.toList()
        val resultMap = LinkedHashMap<String, String>()
        for (i in paramMatchResult.indices) {
            val k = paramMatchResult[i].groups[1]?.value ?: continue
            val v = matchList[i + 1]?.value
            resultMap[k] = v ?: ""
        }
        return resultMap
    }

    override fun toString(): String {
        return "PatternText{ pattern = $pattern}"
    }

    companion object {
        private val argReg: Regex = Regex("""\{\s*(\w+)\s*}""", RegexOption.IGNORE_CASE)
    }
}

