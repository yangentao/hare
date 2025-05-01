package io.github.yangentao.hare.utils

import java.io.File

@Suppress("LiftReturnOrAssignment")
fun joinPath(a: String, b: String, sep: String = "/"): String {
    if (a.isEmpty()) return b
    if (b.isEmpty()) return a
    if (a.endsWith(sep)) {
        if (b.startsWith(sep)) {
            return a + b.substring(1)
        } else {
            return a + b
        }
    } else {
        if (b.startsWith(sep)) {
            return a + b
        } else {
            return "$a$sep$b"
        }
    }
}

object FilePath {
    //tempName(".png") => a1bcd32134.png
    //tempName("png") => a1bcd32134.png
    //tempName("") => a1bcd32134
    fun tempName(ext: String = ""): String {
        return if (ext.isEmpty() || ext.startsWith('.')) {
            uuidString() + ext
        } else {
            uuidString() + "." + ext
        }
    }

    //"/entao/abc.jpg" => abc.jpg
    fun fileName(path: String): String {
        if (path.isEmpty()) return ""
        if (path.last() == '/' || path.last() == '\\') return ""
        if ('/' in path) return path.substringAfterLast('/')
        if ('\\' in path) return path.substringAfterLast('\\')
        return path
    }

    //"/entao/abc.jpg" =>  jpg
    fun ext(path: String): String {
        return fileName(path).substringAfterLast('.', "")
    }

    fun join(vararg ps: String): String {
        val sb = StringBuilder(256)
        for (p in ps) {
            if (p.isEmpty()) continue
            if (sb.isEmpty()) {
                sb.append(p)
            } else {
                if (sb.last() == '/' || sb.last() == '\\') {
                    if (p.first() == '/' || p.first() == '\\') {
                        sb.append(p.substring(1))
                    } else {
                        sb.append(p)
                    }
                } else {
                    if (p.first() == '/' || p.first() == '\\') {
                        sb.append(p)
                    } else {
                        sb.append(File.separatorChar)
                        sb.append(p)
                    }
                }
            }
        }
        return sb.toString()
    }
}
//
//fun main() {
////    println(FilePath.join("/", "entao/", "abc.jpg"))
////    println(FilePath.join("home/", "/entao/", "/abc.jpg"))
////    println(FilePath.join("home/", "", "abc.jpg"))
////    println()
//    println(FilePath.ext("/"))
//    println(FilePath.ext("/entao"))
//    println(FilePath.ext("/entao/"))
//    println(FilePath.ext("/entao/abc"))
//    println(FilePath.ext("/entao/abc.jpg"))
//}