@file:Suppress("unused", "PropertyName", "FunctionName")

package io.github.yangentao.hare

import io.github.yangentao.anno.Name
import io.github.yangentao.anno.Trim
import io.github.yangentao.anno.userName
import io.github.yangentao.hare.actions.Upload
import io.github.yangentao.hare.feature.accountID
import io.github.yangentao.hare.utils.bound
import io.github.yangentao.hare.utils.limitValue
import io.github.yangentao.hare.utils.offsetValue
import io.github.yangentao.hare.utils.queryConditions
import io.github.yangentao.hare.utils.tagContext
import io.github.yangentao.httpbasic.HttpFileParam
import io.github.yangentao.kson.JsonResult
import io.github.yangentao.sql.*
import io.github.yangentao.sql.clause.*
import io.github.yangentao.tag.html.HtmlDiv
import io.github.yangentao.tag.html.HtmlDoc
import io.github.yangentao.types.ICaseSet
import io.github.yangentao.types.Prop
import io.github.yangentao.types.Prop1
import io.github.yangentao.types.decodeValue
import io.github.yangentao.types.returnClass
import io.github.yangentao.types.setPropValue
import io.github.yangentao.types.toICaseSet
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

interface OnHttpContext {
    val context: HttpContext

    fun HttpFileParam.save(subdir: String): Upload? {
        return Upload.fromHttpFile(this.httpFile, context.app.dirUpload, subdir, context.accountID ?: 0L)
    }

    fun htmlDoc(block: HtmlDoc.() -> Unit): HtmlDoc {
        return HtmlDoc(context.tagContext).apply(block)
    }

    fun htmlDiv(block: HtmlDiv.() -> Unit): HtmlDiv {
        return HtmlDiv(context.tagContext).apply(block)
    }

    fun JsonResult.totalOffset(total: Int, offset: Int? = null): JsonResult {
        this.put("total", total)
        this.put("offset", offset ?: offsetValue)
        return this
    }

    fun BaseModelClass<out BaseModel>.fieldNames(): Set<String> {
        return this.tableClass.propertiesHare.map { it.fieldSQL }.toICaseSet()
    }

    fun BaseModelClass<out BaseModel>.queryConditionsCTX(nameSet: Set<String>? = null, nameMap: Map<String, String>? = null): Where? {
        return queryConditions(context.param("q"), nameSet ?: this.fieldNames(), nameMap)
    }

    fun BaseModelClass<out BaseModel>.listByQuery(vararg conditions: Where): JsonResult {
        val pkProp = this.tableClass.primaryKeysHare.firstOrNull()
        val w = AND_ALL(*conditions, this.queryConditions(context.param("q"), this.fieldNames()))
        val ls = this.list(w) {
            orderByCTX(pkProp?.ASC)
            limitByCTX()
        }
        val total: Int = this.count("*", w)
        return ls.jsonResult(total, offsetValue)
    }

    //limit=100
    //offset=0
    fun SQLNode.limitByCTX(defaultLimit: Int? = 200, maxLimit: Int = 500): SQLNode {
        val limit: Int = limitValue ?: defaultLimit ?: return this
        return this.LIMIT_OFFSET(limit.bound(1..maxLimit), offsetValue)
    }

    //asc=name
    //desc=id
    //sort=_id,name_   //id ASC, name DESC
    fun SQLNode.orderByCTX(defaultOrder: String? = null): SQLNode {
        val sort = context.param("sort")
        val sortList = sort?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
        if (!sortList.isNullOrEmpty()) {
            val ls = sortList.map { s ->
                if (s.startsWith('_')) {
                    s.trimStart('_').escapeSQL + " ASC"
                } else if (s.endsWith('_')) {
                    s.trimEnd('_').escapeSQL + " DESC"
                } else {
                    "${s.escapeSQL} ASC"
                }
            }
            return this.ORDER_BY(ls)
        } else {
            val asc: String? = context.param("asc")?.lowercase()
            val desc: String? = context.param("desc")?.lowercase()
            if (asc != null && asc != "null") {
                return this.ORDER_BY(asc.ASC)
            } else if (desc != null && desc != "null") {
                return this.ORDER_BY(desc.DESC)
            } else if (!defaultOrder.isNullOrEmpty()) {
                return this.ORDER_BY(defaultOrder)
            } else {
                //NO order by
            }
        }
        return this
    }

    fun BaseModel.fillPropsCTX(vararg ps: Prop) {
        fromRequest(ps.map { it.userName }.toICaseSet())
    }

    fun BaseModel.fromRequest(keySet: ICaseSet = ICaseSet()) {
        val thisModel = this
        thisModel::class.propertiesHare.forEach {
            val key = it.userName
            val b = if (keySet.isNotEmpty()) {
                context.paramMap.contains(key) && key in keySet
            } else {
                context.paramMap.contains(key)
            }
            if (b) {
                val sval = context.param(key)
                if (sval != null) {
                    val v: String = if (it.hasAnnotation<Trim>()) {
                        sval.trim()
                    } else {
                        sval
                    }
                    it.setPropValue(thisModel, it.decodeValue(v))
                }
            }
        }
    }

    val Prop.valueCTX: Any?
        get() {
            val sv = context.param(this.userName) ?: return null
            return when (this.returnClass) {
                String::class -> sv
                Int::class -> sv.toIntOrNull()
                Long::class -> sv.toLongOrNull()
                Float::class -> sv.toFloatOrNull()
                Double::class -> sv.toDoubleOrNull()
                else -> null
            }
        }

    fun EqualPropsCTX(vararg ps: Prop): Where? {
        return AND_ALL(ps.mapNotNull { it.equalCTX })
    }

    fun EqualAllCTX(vararg ps: Prop): Where? {
        return AND_ALL(ps.mapNotNull { it.equalCTX })
    }

    val Prop.equalCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            return this EQ v
        }

    // %value%
    val Prop1.likeInCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            if (v.toString().isEmpty()) {
                return null
            }
            return this LIKE """%$v%"""
        }

    // value%
    val Prop1.likeCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            if (v.toString().isEmpty()) {
                return null
            }
            return this LIKE """$v%"""
        }

    // %value
    val Prop1._likeCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            if (v.toString().isEmpty()) {
                return null
            }
            return this LIKE """%$v"""
        }

    val Prop1.notEqualCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            return this NE v
        }

    val Prop1.greatEqualCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            return this GE v
        }

    val Prop1.greatCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            return this GT v
        }

    val Prop1.lessEqualCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            return this LE v
        }

    val Prop1.lessCTX: Where?
        get() {
            val v = this.valueCTX ?: return null
            return this LT v
        }

}
