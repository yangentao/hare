@file:Suppress("unused")

package io.github.yangentao.hare.utils

import io.github.yangentao.anno.userName
import kotlin.reflect.KProperty

class AttrStore {
    val map: HashMap<String, Any> = HashMap()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T {
        return map[key] as T
    }

    fun <T> set(key: String, value: T) {
        if (value == null) {
            map.remove(key)
        } else {
            map[key] = value
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> remove(key: String): T? {
        return map.remove(key) as? T
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> getValue(inst: Any, property: KProperty<*>): T {
        return map[property.userName] as T
    }

    operator fun <T> setValue(inst: Any, property: KProperty<*>, value: T) {
        if (value == null) {
            map.remove(property.userName)
        } else {
            map[property.userName] = value
        }
    }
}