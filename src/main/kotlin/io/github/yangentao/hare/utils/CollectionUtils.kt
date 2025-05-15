package io.github.yangentao.hare.utils

import java.util.*

val <T> Array<T>.emptyToNull: Array<T>? get() = if (this.isEmpty()) null else this

@Suppress("ReplaceIsEmptyWithIfEmpty")
val <V, T : Collection<V>> T.emptyToNull: T? get() = if (this.isEmpty()) null else this

fun <T> Collection<T>.toArrayList(): ArrayList<T> {
    return ArrayList(this)
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Collection<Any>.firstTyped(): T? {
    return this.firstOrNull { it is T } as? T
}

fun <T> List<T>.second(): T {
    return this[1]
}

fun <T> List<T>.secondOrNull(): T? {
    return this.getOrNull(1)
}

internal fun <T : Any> Stack<T>.top(): T? {
    if (empty()) return null
    return peek()
}