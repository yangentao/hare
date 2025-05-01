@file:Suppress("unused")

package io.github.yangentao.hare.utils

import java.io.File

@Suppress("DEPRECATION")
val Thread.isMain: Boolean get() = if (javaVersionInt >= 19) this.threadId() == 1L else this.id == 1L

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

@Suppress("RecursivePropertyAccessor")
val Throwable.rootError: Throwable
    get() {
        return this.cause?.rootError ?: this
    }

fun File.ensureDirs(): File {
    if (!this.exists()) {
        this.mkdirs()
    }
    return this
}

const val HTML404 = "<!doctype html><html><head><title>test</title></head><body><center>404 No Resource Found!</center></body></html>"