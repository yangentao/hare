@file:Suppress("SameParameterValue")

package io.github.yangentao.hare


import io.github.yangentao.hare.utils.top
import io.github.yangentao.sql.BaseModel
import io.github.yangentao.sql.BaseModelClass
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.fieldSQL
import io.github.yangentao.sql.modelFieldSQL
import io.github.yangentao.types.ICaseMap
import io.github.yangentao.types.ICaseSet
import io.github.yangentao.types.decodeValue
import io.github.yangentao.types.toICaseMap
import io.github.yangentao.types.toICaseSet
import java.util.*
import kotlin.reflect.KProperty

//q="a|in|1|2|3, {b|le|2,c|le|3}, d|ge|5, [e|lt|7,g|ne|8]"
fun BaseModelClass<out BaseModel>.queryConditions(query: String?, nameSet: Set<String>? = null, nameMap: Map<String, String>? = null): Where? {
    if (query.isNullOrEmpty()) return null
    val node = CondParse(query).parse() ?: return null
    return makeWhere(node, nameSet?.toICaseSet(), nameMap?.toICaseMap())
}

private fun BaseModelClass<out BaseModel>.makeWhere(node: CondParse.Node, nameSet: ICaseSet?, nameMap: ICaseMap<String>?): Where? {
    if (node is CondParse.Item) {
        if (nameSet != null && nameSet.isNotEmpty() && node.key !in nameSet) return null
        return evalCondition(nameMap?.get(node.key) ?: node.key, node.op, node.value)
    }
    if (node is CondParse.Group) {
        val ls = node.items.map { makeWhere(it, nameSet, nameMap) }
        return if (node.isAnd) AND_ALL(ls) else OR_ALL(ls)
    }
    return null
}

private val opSet: Set<String> = setOf("eq", "ge", "le", "gt", "lt", "ne", "nul", "start", "end", "contain", "in", "bit")

//"name|eq|entao"
//key and eq MUST lowercased
private fun BaseModelClass<out BaseModel>.evalCondition(key: String, oper: String, value: String): Where? {
    val op = oper.trim().lowercase()
    if (op !in opSet) return null
//    val lowKey = key.lowercase().trim().filter { it.isLetterOrDigit() || it == '_' }
    val prop: KProperty<*> = propsHare.firstOrNull { it.fieldSQL.lowercase() == key.lowercase() } ?: return null
    val lowKey = prop.modelFieldSQL
    val ret = prop.returnType.classifier ?: return null;
    if (op == "nul") {
        if (ret == String::class) {
            return lowKey EQ "" OR IS_NULL(lowKey)
        }
        return IS_NULL(lowKey)
    }
    if (value.isEmpty()) return null
    if (op == "in") {
        when (ret) {
            String::class -> return lowKey IN value.split(',')
            Int::class -> return lowKey IN value.split(',').map { it.toInt() }
            Long::class -> return lowKey IN value.split(',').map { it.toLong() }
        }
        return null
    }

    if (ret == String::class) {
        when (op) {
            "start" -> return lowKey LIKE "$value%"
            "end" -> return lowKey LIKE "%$value"
            "contain" -> return lowKey LIKE "%$value%"
        }
    }

    val newValue: Any = prop.decodeValue(value) ?: return null
    return when (op) {
        "eq" -> lowKey EQ newValue
        "ne" -> lowKey NE newValue
        "gt" -> lowKey GT newValue
        "lt" -> lowKey LT newValue
        "ge" -> lowKey GE newValue
        "le" -> lowKey LE newValue
        "bit" -> {
            if (newValue is Number) {
                lowKey HAS_ALL_BITS newValue.toInt()
            } else null
        }

        else -> null
    }
}

private class CondParse(query: String) {
    private val buf: String = query.trim()
    private var index: Int = 0
    private val root = Group('{')
    private var currentGroup: Group? = root
    private var condStartIndex: Int = 0

    private fun preCharIs(vararg cs: Char?): Boolean {
        for (i in index - 1 downTo 0) {
            if (buf[i].isWhitespace()) continue
            return buf[i] in cs
        }
        return null in cs
    }

    private fun groupStart() {
        checkItem()
        val ch = buf[index]
        val g = Group(ch)
        currentGroup?.add(g)
        currentGroup = g
        condStartIndex = index + 1
    }

    private fun groupEnd() {
        checkItem()
        val p = currentGroup?.parent
        val group = currentGroup
        if (group != null && p != null) {
            if (group.items.size == 0) {
                p.items.remove(group)
            } else if (group.items.size == 1) {
                val item = group.items.first()
                val n = p.items.indexOf(group)
                p.items[n] = item
                item.parent = p
            }
        }
        currentGroup = p
        condStartIndex = index + 1
    }

    private fun checkItem() {
        val s = buf.substring(condStartIndex, index).trim { it.isWhitespace() || it == ',' }
        if (s.isNotEmpty()) {
            s.split(',').forEach { sub ->
                val ls = sub.split('|')
                when (ls.size) {
                    2 -> currentGroup?.add(Item(ls[0].lowercase().trim(), ls[1].lowercase().trim(), ""))
                    3 -> currentGroup?.add(Item(ls[0].lowercase().trim(), ls[1].lowercase().trim(), ls[2].trim()))
                    in 4..50 -> currentGroup?.add(Item(ls[0].lowercase().trim(), ls[1].lowercase().trim(), ls.slice(2..<ls.size).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")))
                }
            }

        }
    }

    fun parse(): Node? {
        if (buf.isEmpty()) return null
        val stack = Stack<Pair<Char, Int>>()
        while (index < buf.length) {
            val ch = buf[index]
            when (ch) {
                '[' -> {
                    if (preCharIs(null, '[', '{', ',')) {
                        groupStart()
                        stack.push(ch to index)
                    }
                }

                '{' -> {
                    if (preCharIs(null, '[', '{', ',')) {
                        groupStart()
                        stack.push(ch to index)
                    }
                }

                ']' -> {
                    val top = stack.top()
                    if (top?.first == '[') {
                        groupEnd()
                        stack.pop()
                    }
                }

                '}' -> {
                    val top = stack.top()
                    if (top?.first == '{') {
                        groupEnd()
                        stack.pop()
                    }
                }
            }
            index += 1
        }
        checkItem()
        if (root.items.isEmpty()) {
            return null
        }
        if (root.items.size == 1) {
            return root.items.first()
        }
        return root
    }

    abstract class Node(var parent: Group? = null)
    class Group(val ch: Char) : Node() {
        val items: ArrayList<Node> = ArrayList()

        val isAnd: Boolean get() = ch == '{'

        fun add(node: Node) {
            node.parent = this
            items += node
        }

        override fun toString(): String {
            return if (ch == '{') {
                "AND( ${items.joinToString(", ")} )"
            } else {
                "OR( ${items.joinToString(", ")} )"
            }
        }
    }

    class Item(val key: String, val op: String, val value: String) : Node() {
        override fun toString(): String {
            return "$key $op $value"
        }
    }

}
//
//fun main() {
////    val q = "{a|EQ|1,{b|GE|2{, c|eq|4},[a|eq|1, b|ge|2} ] , c|le|3}"
//    testCC(" {a_2-c|in|1|2|3, {b|le|2,c|le|3}, d|ge|5, [e|lt|7,g|ne|8]} ")
//}
//
//private fun testCC(q: String) {
//    println("query: $q ")
//    val c = CondParse(q)
//    val node = c.parse()
//    println(node)
//    println()
//}
