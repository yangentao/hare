package io.github.yangentao.hare.log

import io.github.yangentao.kson.KsonValue
import java.io.PrintWriter
import java.io.StringWriter

fun anyArrayToString(args: Array<out Any?>): String {
    return args.joinToString(" ") {
        anyToString(it)
    }
}

fun anyToString(obj: Any?): String {
    if (obj == null) {
        return "null"
    }
    if (obj is String) {
        return obj
    }
    if (obj.javaClass.isPrimitive) {
        return obj.toString()
    }

    if (obj is Throwable) {
        val sw = StringWriter(512)
        val pw = PrintWriter(sw)
        obj.printStackTrace(pw)
        return sw.toString()
    }
    if (obj is KsonValue) return obj.toString()

    return when (obj) {
        is CharArray -> "CharArray[${obj.joinToString(",") { anyToString(it) }}]"
        is BooleanArray -> "BoolArray[${obj.joinToString(",") { anyToString(it) }}]"
        is ShortArray -> "ShortArray[${obj.joinToString(",") { anyToString(it) }}]"
        is IntArray -> "IntArray[${obj.joinToString(",") { anyToString(it) }}]"
        is LongArray -> "LongArray[${obj.joinToString(",") { anyToString(it) }}]"
        is FloatArray -> "FloatArray[${obj.joinToString(",") { anyToString(it) }}]"
        is DoubleArray -> "DoubleArray[${obj.joinToString(",") { anyToString(it) }}]"
        is Array<*> -> "Array[${obj.joinToString(",") { anyToString(it) }}]"
        is List<*> -> "List[${obj.joinToString(",") { anyToString(it) }}]"
        is Set<*> -> "Set[${obj.joinToString(",") { anyToString(it) }}]"
        is Map<*, *> -> "Map[${obj.map { "${anyToString(it.key)} = ${anyToString(it.value)}" }.joinToString(",")}}]"
        is Iterable<*> -> "Iterable[${obj.joinToString(",") { anyToString(it) }}]"
        else -> obj.toString()
    }

}