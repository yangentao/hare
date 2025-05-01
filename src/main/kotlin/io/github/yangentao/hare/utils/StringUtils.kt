package io.github.yangentao.hare.utils

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.*

fun String.encodedURL(charset: Charset = Charsets.UTF_8): String {
    return URLEncoder.encode(this, charset)
}

val String.encodedURL: String
    get() {
        return URLEncoder.encode(this, Charsets.UTF_8)
    }
val String.decodedURL: String
    get() {
        return URLDecoder.decode(this, Charsets.UTF_8)
    }

val String.decodedBase64: String
    get() {
        if (this.isEmpty()) {
            return ""
        }
        val ba = Base64.getUrlDecoder().decode(this)
        return String(ba, Charsets.UTF_8)
    }
val String.encodedBase64: String
    get() {
        if (this.isEmpty()) {
            return ""
        }
        return Base64.getUrlEncoder().encodeToString(this.toByteArray())
    }

//ignore case NOT equal
infix fun String?.ine(other: String?): Boolean {
    return !this.equals(other, ignoreCase = true)
}

//ignore case equal
infix fun String?.ieq(other: String?): Boolean {
    return this.equals(other, ignoreCase = true)
}

//ignore case equal
infix fun String.istart(other: String): Boolean {
    return this.startsWith(other, ignoreCase = true)
}

//ignore case equal
infix fun String.iend(other: String): Boolean {
    return this.endsWith(other, ignoreCase = true)
}

fun notBlankOf(last: String, vararg ls: String?): String {
    for (s in ls) {
        if (s != null && s.isNotEmpty()) return s
    }
    return last
}

//3168438f37474896a68693044df913fa
fun uuidString(): String {
    val uuid = UUID.randomUUID().toString()
    return uuid.replace("-", "")
}

@Suppress("ReplaceIsEmptyWithIfEmpty")
val String.emptyToNull: String? get() = if (this.isEmpty()) null else this

@Suppress("ReplaceIsEmptyWithIfEmpty")
val String.blankToNull: String? get() = if (this.isBlank()) null else this

val String.toRegexIC: Regex get() = this.toRegex(RegexOption.IGNORE_CASE)
fun String.toBooleanValue(trueValues: List<String> = listOf("true", "yes", "1"), falseValues: List<String> = listOf("false", "no", "0")): Boolean? {
    for (t in trueValues) {
        if (this ieq t) return true
    }
    for (f in falseValues) {
        if (this ieq f) return false
    }
    return null
}

fun String.head(n: Int): String {
    if (n <= 0) {
        return ""
    }
    if (this.length <= n) {
        return this
    }
    return this.substring(0, n)
}

fun String.tail(n: Int): String {
    if (n <= 0) {
        return ""
    }
    if (this.length < n) {
        return this
    }
    return this.substring(this.length - n)
}

val String.quoted: String get() = if (this.startsWith("\"") && this.endsWith("\"")) this else "\"$this\"";

// a => 'a'
val String.quotedSingle: String
    get() {
        if (this.startsWith("'") && this.endsWith("'")) return this
        return "'$this'"
    }