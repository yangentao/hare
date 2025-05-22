package io.github.yangentao.hare.utils

import io.github.yangentao.sql.BaseModel
import io.github.yangentao.sql.BaseModelClass
import io.github.yangentao.sql.clause.*
import io.github.yangentao.sql.fieldSQL
import io.github.yangentao.sql.modelFieldSQL
import io.github.yangentao.types.*
import kotlin.reflect.KProperty

//q="a|in|1|2|3, {b|le|2,c|le|3}, d|ge|5, [e|lt|7,g|ne|8]"
fun BaseModelClass<out BaseModel>.queryConditions(query: String?, limitFields: Set<String>? = null, renameMap: Map<String, String>? = null): Where? {
    if (query.isNullOrEmpty()) return null
    val node = ConditionParser(query).parse() ?: return null
    return makeWhere(node, limitFields?.toICaseSet(), renameMap?.toICaseMap())
}

private fun BaseModelClass<out BaseModel>.makeWhere(node: CondItem, limitNames: ICaseSet?, nameMap: ICaseMap<String>?): Where? {
    when (node) {
        is FieldCond -> {
            if (limitNames != null && limitNames.isNotEmpty() && node.field !in limitNames) return null
            return evalCondition(nameMap?.get(node.field) ?: node.field, node.op.lowercase(), node.values)
        }

        is AndCond -> {
            val ls = node.items.map { makeWhere(it, limitNames, nameMap) }
            return AND_ALL(ls)
        }

        is OrCond -> {
            val ls = node.items.map { makeWhere(it, limitNames, nameMap) }
            return OR_ALL(ls)
        }
    }
}

private fun BaseModelClass<out BaseModel>.evalCondition(key: String, op: String, values: List<String>): Where? {
    val prop: KProperty<*> = propsHare.firstOrNull { it.fieldSQL ieq key } ?: return null
    val lowKey = prop.modelFieldSQL
    val propClass = prop.returnType.classifier ?: return null
    when (op) {
        "nul" -> {
            if (propClass == String::class) {
                return lowKey EQ "" OR IS_NULL(lowKey)
            }
            return IS_NULL(lowKey)
        }

        "in" -> {
            if (values.isEmpty()) return null
            when (propClass) {
                String::class -> return lowKey IN values
                Int::class -> return lowKey IN values.map { it.toInt() }
                Long::class -> return lowKey IN values.map { it.toLong() }
            }
            return null
        }

        "start" -> return if (propClass == String::class) lowKey LIKE "${values.first()}%" else null
        "end" -> return if (propClass == String::class) lowKey LIKE "%${values.first()}" else null
        "contain" -> return if (propClass == String::class) lowKey LIKE "%${values.first()}%" else null

        "eq" -> return prop.decodeValue(values.first())?.let { lowKey EQ it }
        "ne" -> return prop.decodeValue(values.first())?.let { lowKey NE it }
        "gt" -> return prop.decodeValue(values.first())?.let { lowKey GT it }
        "lt" -> return prop.decodeValue(values.first())?.let { lowKey LT it }
        "ge" -> return prop.decodeValue(values.first())?.let { lowKey GE it }
        "le" -> return prop.decodeValue(values.first())?.let { lowKey LE it }
        "bit" -> {
            val newValue = prop.decodeValue(values.first())
            return if (newValue is Number) {
                lowKey HAS_ALL_BITS newValue.toInt()
            } else null
        }

        else -> return null
    }

}