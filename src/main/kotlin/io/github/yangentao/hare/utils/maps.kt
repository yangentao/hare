package io.github.yangentao.hare.utils

import java.util.*

fun <K, V> MutableMap<K, java.util.ArrayList<V>>.appendValue(key: K, value: V) = getOrPut(key) { ArrayList() }.add(value)
fun <K, V> MutableMap<K, ArrayList<V>>.appendAll(key: K, value: Iterable<V>) = getOrPut(key) { ArrayList() }.addAll(value)
fun <K, V> MutableMap<K, ArrayList<V>>.listValue(key: K): ArrayList<V> = getOrPut(key) { ArrayList() }
fun <K, V> MutableMap<K, ArrayList<V>>.firstValue(key: K): V? = this[key]?.firstOrNull()
fun <K, V> MutableMap<K, ArrayList<V>>.getValue(key: K): V? = this[key]?.firstOrNull()
fun <K, V> MutableMap<K, ArrayList<V>>.setValue(key: K, value: V) {
    this[key] = arrayListOf(value)
}

class ListMap<K, V>(map: Map<K, ArrayList<V>>? = null, private val hashMap: java.util.LinkedHashMap<K, ArrayList<V>> = LinkedHashMap()) : MutableMap<K, ArrayList<V>> by hashMap {
    init {
        if (map != null) hashMap.putAll(map)
    }
}

class ICaseListMap<V>(map: Map<String, ArrayList<V>>? = null, private val imap: ICaseMap<ArrayList<V>> = ICaseMap()) : MutableMap<String, ArrayList<V>> by imap {
    init {
        if (map != null) this.putAll(map)
    }
}

class ICaseMap<V>(map: Map<String, V>? = null, pairs: Iterable<Pair<String, V>>? = null, private val treeMap: TreeMap<String, V> = TreeMap<String, V>(String.CASE_INSENSITIVE_ORDER)) :
    MutableMap<String, V> by treeMap {
    init {
        if (map != null) putAll(map)
        if (pairs != null) putAll(pairs)
    }
}

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
class ICaseSet(elements: Iterable<String>? = null, private val treeSet: TreeSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)) : MutableSet<String> by treeSet {
    init {
        if (elements != null) addAll(elements)
    }
}

fun <V> Map<String, V>.toICaseMap() = this as? ICaseMap<V> ?: ICaseMap(this)

fun Iterable<String>.toICaseSet(): ICaseSet = this as? ICaseSet ?: ICaseSet(this)

fun icaseSet(vararg elements: String): ICaseSet = ICaseSet().apply { addAll(elements) }

fun <V> icaseMap(vararg elements: Pair<String, V>): ICaseMap<V> = ICaseMap<V>().apply { putAll(elements) }

fun isetOf(vararg elements: String): ICaseSet = icaseSet(*elements)

fun <V> imapOf(vararg pairs: Pair<String, V>): ICaseMap<V> = icaseMap(*pairs)

//ignore case map
class ICaseTreeMap<V> : TreeMap<String, V>(String.CASE_INSENSITIVE_ORDER)
