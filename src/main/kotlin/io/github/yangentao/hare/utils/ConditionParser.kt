package io.github.yangentao.hare.utils

import io.github.yangentao.charcode.CharCode
import io.github.yangentao.charcode.TextScanner
import io.github.yangentao.types.sublist

class ConditionParser(val text: String) {
    private val ts: TextScanner = TextScanner(text)

    fun parse(): CondItem? {
        ts.skipSpaceTab()
        if (ts.isEnd) return null
        val item = when (ts.nowChar) {
            '{' -> parseAnd()
            '[' -> parseOr()
            else -> parseAnd(true)
        }
        ts.skipSpaceTab()
        if (!ts.isEnd) error("parse errror: ${ts.position}, left: ${ts.leftText}")
        return item
    }

    private fun parseValue(): CondItem? {
        ts.skipSpaceTab()
        if (ts.isEnd) return null
        val item = when (ts.nowChar) {
            '{' -> parseAnd()
            '[' -> parseOr()
            else -> parseItem()
        }
        return item
    }

    private fun parseAnd(noBrace: Boolean = false): AndCond? {
        val list: ArrayList<CondItem> = ArrayList()
        ts.skipSpaceTab()
        if (!noBrace) ts.expectChar('{')
        while (!ts.isEnd) {
            if (ts.nowChar == '}') break
            val item = parseValue() ?: break
            list.add(item)
            val ls = ts.skipChars(listOf(CharCode.SP, CharCode.TAB, ','))
            if (',' !in ls) {
                break
            }
        }
        ts.skipSpaceTab()
        if (!noBrace) ts.expectChar('}')
        if (list.isEmpty()) return null
        return AndCond(list)
    }

    private fun parseOr(): OrCond? {
        val list: ArrayList<CondItem> = ArrayList()
        ts.skipSpaceTab()
        ts.expectChar('[')
        while (!ts.isEnd) {
            if (ts.nowChar == ']') break
            val item = parseValue() ?: break
            list.add(item)
            val ls = ts.skipChars(listOf(CharCode.SP, CharCode.TAB, ','))
            if (',' !in ls) {
                break
            }
        }
        ts.skipSpaceTab()
        ts.expectChar(']')
        if (list.isEmpty()) return null
        return OrCond(list)
    }

    private fun parseItem(): FieldCond? {
        ts.skipSpaceTab()
        val ls = ts.moveUntil(listOf(',', ']', '}'))
        if (ls.isEmpty()) return null
        val s = String(ls.toCharArray())
        val list = s.split('|')
        return FieldCond(list.first().trim(), list.second().trim(), list.sublist(2).map { it.trim() })
    }
}

sealed interface CondItem {
}

class FieldCond(val field: String, val op: String, val values: List<String>) : CondItem {
    override fun toString(): String {
        return "Cond($field, $op, $values)"
    }

    override fun equals(other: Any?): Boolean {
        if (other is FieldCond) {
            return this.field == other.field && this.op == other.op && this.values == other.values
        }
        return false
    }

    override fun hashCode(): Int {
        var result = field.hashCode()
        result = 31 * result + op.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }
}

class AndCond(val items: List<CondItem>) : CondItem {
    override fun toString(): String {
        return "AND($items)"
    }
}

class OrCond(val items: List<CondItem>) : CondItem {
    override fun toString(): String {
        return "OR($items)"
    }
}