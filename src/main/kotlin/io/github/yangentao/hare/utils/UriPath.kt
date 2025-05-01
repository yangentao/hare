package io.github.yangentao.hare.utils



data class UriPath(val value: String) : Comparable<UriPath> {

    // "aa/bb".prefix("aa") => true
    // "aa/bb".prefix("a") => false
    fun hasPrefix(path: String): Boolean {
        if (!value.startsWith(path, ignoreCase = true)) return false
        if (value.length == path.length) return true
        return path.endsWith('/') || value[path.length] == '/'
    }

    //("/static", "/static/a.png") => a.png
    //("/static", "/static/") => ""
    //("/static", "/static") => ""
    //("/static", "/stati") => null
    fun trimStartPath(parent: String): String? {
        if (!value.startsWith(parent, ignoreCase = true)) return null
        val sub = value.substring(parent.length)
        if (sub.startsWith('/')) return sub.substring(1)
        return sub
    }

    override fun compareTo(other: UriPath): Int {
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is String -> value.equals(other, ignoreCase = true)
            is UriPath -> value.equals(other.value, ignoreCase = true)
            is CharSequence -> value.equals(other.toString(), ignoreCase = true)
            else -> false
        }
    }

    companion object {
        //忽略掉空字符串
        fun join(vararg ps: String): String {
            val sb = StringBuilder(256)
            for (p in ps) {
                if (p.isEmpty()) continue
                if (sb.isEmpty()) {
                    sb.append(p)
                    continue
                }
                if (sb.last() == '/') {
                    if (p.first() == '/') {
                        sb.append(p.substring(1))
                    } else {
                        sb.append(p)
                    }
                } else {
                    if (p.first() == '/') {
                        sb.append(p)
                    } else {
                        sb.append('/')
                        sb.append(p)
                    }
                }

            }
            return sb.toString()
        }
    }
}