package io.github.yangentao.hare.utils

fun Int.bound(range: IntRange): Int {
    if (this < range.start) return range.start
    if (this > range.endInclusive) return range.endInclusive
    return this
}

fun Long.bound(range: LongRange): Long {
    if (this < range.start) return range.start
    if (this > range.endInclusive) return range.endInclusive
    return this
}
