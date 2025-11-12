@file:Suppress("unused")

package io.github.yangentao.hare.utils

import io.github.yangentao.types.tid
import java.io.File

val Thread.isMain: Boolean get() = this.tid == 1L

fun AutoCloseable.closeSafe() {
    try {
        this.close()
    } catch (_: Throwable) {
    }
}

inline fun <R> quiet(block: () -> R): R? {
    try {
        return block()
    } catch (_: Exception) {
    }
    return null
}

val Throwable.rootError: Throwable
    get() {
        return this.cause?.rootError ?: this
    }
val Throwable.rootMessage: String
    get() {
        return this.rootError.message ?: this.rootError.toString()
    }

fun File.ensureDirs(): File {
    if (!this.exists()) {
        this.mkdirs()
    }
    return this
}



const val HTML404 = "<!doctype html><html><head><title>test</title></head><body><center>404 No Resource Found!</center></body></html>"