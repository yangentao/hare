@file:Suppress("unused")

package io.github.yangentao.hare

import io.github.yangentao.anno.userName
import io.github.yangentao.httpbasic.HttpStatus
import io.github.yangentao.types.decodeValue
import kotlin.reflect.KProperty

//[start, end]
class FileRange(val start: Long, val end: Long) {
    val size: Long get() = end - start + 1
}

class StatusException(message: String, val code: Int = -1, val status: HttpStatus = statusByECode(code), val data: Any? = null) : Exception(message) {
    override fun toString(): String {
        return "$code , $message"
    }

    companion object {
        var defaultStatus: HttpStatus = HttpStatus.OK
        var autoStatus: Boolean = false
    }
}

fun errorStatus(message: String, code: Int = -1, status: HttpStatus? = null, data: Any? = null): Nothing {
    throw StatusException(message, code, status ?: statusByECode(code), data)
}

fun statusByECode(code: Int): HttpStatus {
    if (StatusException.autoStatus && code in 400..599) return HttpStatus.valueOf(code)
    return StatusException.defaultStatus
}

object ContextAttributeOr {
    operator fun <T : Any> getValue(thisRef: HttpContext, property: KProperty<*>): T? {
        return thisRef.getAttr(property.userName)
    }

    operator fun <T : Any> setValue(thisRef: HttpContext, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.removeAttr(property.userName)
        } else {
            thisRef.putAttr(property.userName, value)
        }
    }
}

class ContextAttribute<T : Any>(val onMiss: (HttpContext) -> T) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: HttpContext, property: KProperty<*>): T {
        return thisRef.attributes.getOrPut(property.userName) { onMiss(thisRef) } as T
    }

    operator fun setValue(thisRef: HttpContext, property: KProperty<*>, value: T) {
        thisRef.attributes[property.userName] = value
    }
}

object HttpAttributeOr {
    operator fun <T : Any> getValue(thisRef: OnHttpContext, property: KProperty<*>): T? {
        return thisRef.context.getAttr(property.userName)
    }

    operator fun <T : Any> setValue(thisRef: OnHttpContext, property: KProperty<*>, value: T?) {
        if (value == null) {
            thisRef.context.removeAttr(property.userName)
        } else {
            thisRef.context.putAttr(property.userName, value)
        }
    }
}

class HttpAttribute<T : Any>(val fallback: (OnHttpContext) -> T) {
    operator fun getValue(thisRef: OnHttpContext, property: KProperty<*>): T {
        return thisRef.context.getAttrOrPut(property.userName) { fallback(thisRef) }
    }

    operator fun setValue(thisRef: OnHttpContext, property: KProperty<*>, value: T) {
        thisRef.context.putAttr(property.userName, value)
    }
}

object HttpParameterOr {
    operator fun <T> getValue(thisRef: OnHttpContext, property: KProperty<*>): T? {
        val s = thisRef.context.param(property.userName) ?: return null
        @Suppress("UNCHECKED_CAST")
        return property.decodeValue(s) as? T
    }
}

class HttpParameter<T : Any>(val defaultValue: T) {
    operator fun getValue(thisRef: OnHttpContext, property: KProperty<*>): T {
        val s = thisRef.context.param(property.userName) ?: return defaultValue
        @Suppress("UNCHECKED_CAST")
        return property.decodeValue(s) as? T ?: defaultValue
    }
}

object HttpHeaderValueOr {
    operator fun <T : Any> getValue(thisRef: OnHttpContext, property: KProperty<*>): T? {
        val s = thisRef.context.requestHeader(property.userName) ?: return null
        @Suppress("UNCHECKED_CAST")
        return property.decodeValue(s) as? T
    }
}

class HttpHeaderValue<T : Any>(val defaultValue: T) {
    operator fun getValue(thisRef: OnHttpContext, property: KProperty<*>): T {
        val s = thisRef.context.requestHeader(property.userName) ?: return defaultValue
        @Suppress("UNCHECKED_CAST")
        return property.decodeValue(s) as? T ?: defaultValue
    }
}