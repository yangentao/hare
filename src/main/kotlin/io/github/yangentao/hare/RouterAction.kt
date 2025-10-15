@file:Suppress("UNCHECKED_CAST")

package io.github.yangentao.hare

import io.github.yangentao.anno.Length
import io.github.yangentao.anno.SepChar
import io.github.yangentao.anno.Trim
import io.github.yangentao.anno.userName
import io.github.yangentao.config.ConfigList
import io.github.yangentao.config.ConfigMap
import io.github.yangentao.config.Configs
import io.github.yangentao.hare.utils.blankToNull
import io.github.yangentao.hare.utils.emptyToNull
import io.github.yangentao.hare.utils.second
import io.github.yangentao.hare.utils.secondOrNull
import io.github.yangentao.hare.utils.toArrayList
import io.github.yangentao.hare.utils.toBooleanValue
import io.github.yangentao.httpbasic.HttpFile
import io.github.yangentao.httpbasic.HttpFileParam
import io.github.yangentao.httpbasic.HttpMethod
import io.github.yangentao.kson.Kson
import io.github.yangentao.kson.KsonArray
import io.github.yangentao.kson.KsonObject
import io.github.yangentao.types.*
import java.io.File
import java.nio.charset.Charset
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

typealias LambdaAction = Function1<HttpContext, Any?>

// 路由,
// RouterAction与RouterInterceptor的区别是返回值
// lambda, fun(context:HttpContext):Any? {}
// kfunction, 可以返回任意值.  fun(context:HttpContext, a:X, b:Y, ...):Any? {}
class RouterAction(val match: UriMatch, val action: Function<Any?>, val beforeList: List<KFunction<*>> = emptyList(), val afterList: List<KFunction<*>> = emptyList(), val group: KClass<*>? = null) {
    val isKFunction: Boolean = action is KFunction<*>
    val lambda: LambdaAction?

    val kfunction: KFunction<Any?>?
    val ownerObject: Any?
    val ownerClass: KClass<*>?
    val actionAnnotations: List<Annotation>
    val groupAnnotations: List<Annotation> = group?.annotations ?: emptyList()
    val allAnnos: List<Annotation>
    val actionAnno: Action?

    val charset: Charset by lazy { actionAnno?.charset?.blankToNull?.let { Charset.forName(it) } ?: Charsets.UTF_8 }
    val mime: String? by lazy { actionAnno?.mime?.blankToNull }

    val methods: Set<String> by lazy { actionAnno?.methods?.emptyToNull?.toSet() ?: defaultMethods }

    init {

        if (isKFunction) {
            val kfun = action as KFunction<Any?>
            kfunction = kfun
            lambda = null
        } else {
            kfunction = null
            lambda = action as LambdaAction
        }
        ownerObject = kfunction?.ownerObject
        ownerClass = kfunction?.ownerClass
        actionAnnotations = kfunction?.annotations ?: emptyList()
        allAnnos = actionAnnotations + groupAnnotations
        actionAnno = kfunction?.findAnnotation()
    }

    fun invoke(context: HttpContext): Any? {
        if (isKFunction) {
            val inst: Any? = context.instanceActionGroup(ownerClass, ownerObject)
            beforeList.forEach {
                invokeKFunction(context, it, inst, mapOf(this::class to this))
            }
            val r: Any? = if (context.commited) Unit else invokeKFunction(context, kfunction!!, inst)
            afterList.forEach {
                invokeKFunction(context, it, inst, mapOf(this::class to this))
            }
            return r
        } else {
            val v = lambda?.invoke(context)
            return v
        }
    }

    override fun toString(): String {
        if (isKFunction) return "RouterAction{ action=${kfunction?.description()}  }"
        return "RouterAction{ action=$lambda}"
    }

    companion object {
        val defaultMethods = setOf(HttpMethod.GET, HttpMethod.POST)
    }
}

//拦截器, 返回值只能是Unit.  但是RouterAction的kfunction可以返回任意类型
// lambda,  fun(context:HttpContext, action:RouterAction):Unit {}
// fun beforeXXX(context:HttpContext, action:RouterAction):Unit
class RouterInterceptor(val action: Function<Unit>) {
    val isKFunction: Boolean = action is KFunction<*>
    val lambda: Function1<HttpContext, Unit>?

    val kfunction: KFunction<Unit>?
    val ownerObject: Any?
    val ownerClass: KClass<*>?
    val annotations: List<Annotation>

    init {
        if (isKFunction) {
            val kfun = action as KFunction<Unit>
            kfunction = kfun
            lambda = null
        } else {
            lambda = action as Function1<HttpContext, Unit>
            kfunction = null
        }
        ownerObject = kfunction?.ownerObject
        ownerClass = kfunction?.ownerClass
        annotations = kfunction?.annotations ?: emptyList()
    }

    fun invoke(context: HttpContext, classValueMap: Map<KClass<*>, Any> = emptyMap()) {
        if (isKFunction) {
            val inst: Any? = context.instanceActionGroup(ownerClass, ownerObject)
            invokeKFunction(context, kfunction!!, inst, classValueMap)
        } else {
            lambda?.invoke(context)
        }
    }

    override fun toString(): String {
        if (isKFunction) return "RouterInterceptor{ action=$kfunction}"
        return "RouterInterceptor{ action=$lambda}"
    }
}

private fun HttpContext.instanceActionGroup(ownerClass: KClass<*>?, ownerObject: Any?): Any? {
    if (ownerObject != null) return ownerObject
    if (ownerClass != null) {
        val obj: Any = ownerClass.objectInstance ?: ownerClass.tryCreateInstance(this) ?: ownerClass.createInstance()
        return obj
    }
    return null
}

private fun invokeKFunction(context: HttpContext, kfun: KFunction<*>, inst: Any?, classValueMap: Map<KClass<*>, Any> = emptyMap()): Any? {
    val map = prepareParamsMap(context, kfun, inst, classValueMap)
    try {
        return kfun.callBy(map)
    } catch (ex: java.lang.reflect.InvocationTargetException) {
        throw ex.cause ?: ex
    }
}

private fun prepareParamsMap(context: HttpContext, func: KFunction<*>, inst: Any?, classValueMap: Map<KClass<*>, Any> = emptyMap()): HashMap<KParameter, Any?> {
    val map = HashMap<KParameter, Any?>()

    fun putValue(p: KParameter, value: Any?) {
        if (value != null || p.type.isMarkedNullable) {
            map[p] = value
            return
        }
        if (p.isOptional) {
            return
        }
        error("参数缺失: param: ${p.name}, $p,\n action: $func, \n uri: ${context.requestUri}\n paramMap: ${context.paramMap.entries.joinToString(";") { it.key + "=" + it.value }}")
    }
    for (param in func.parameters) {
        when (param.kind) {
            KParameter.Kind.INSTANCE, KParameter.Kind.EXTENSION_RECEIVER -> {
                if (inst == null) error("NO instance argument given! $func ")
                map[param] = inst
            }

            KParameter.Kind.VALUE -> {
                val paramName = param.userName
                if (classValueMap.containsKey(param.type.classifier)) {
                    val v = classValueMap[param.type.classifier]
                    putValue(param, v)
                    continue
                }

                when (param.type.classifier) {
                    HttpContext::class -> map[param] = context
                    HttpFileParam::class -> putValue(param, context.fileUpload(paramName))
                    HttpFile::class -> putValue(param, context.fileUpload(paramName)?.httpFile)
                    File::class -> putValue(param, context.fileUpload(paramName)?.file)
                    else -> {
                        val vls = context.paramList(paramName)
                        val v = if (vls.size == 1) {
                            val newValue = validateParam(param, vls.first())
                            ParameterConverter(param).fromArray(listOf(newValue))
                        } else {
                            ParameterConverter(param).fromArray(vls)
                        }
                        putValue(param, v)
                    }
                }
            }
        }
    }
    return map
}

class ParameterConverter(val param: KParameter) {
    private val paramClass: KClass<*> = param.type.classifier as KClass<*>

    @Suppress("unused")
    private val isGeneric: Boolean = param.type.isGeneric
    private val firstArgumentClass: KClass<*>? = param.type.firstGenericArgumentClass
    private val secondArgumentClass: KClass<*>? = param.type.secondGenericArgumentClass

    fun fromArray(valueList: List<String>): Any? {
        if (valueList.isEmpty()) return null
        when (paramClass) {
            List::class, ArrayList::class -> {
                val sepChar = param.findAnnotation<SepChar>()
                if (sepChar != null) {
                    assert(valueList.size == 1)
                    return valueList.first().split(sepChar.list).mapNotNull { stringToSingle(firstArgumentClass!!, it) }.toArrayList()
                } else {
                    return valueList.mapNotNull { stringToSingle(firstArgumentClass!!, it) }.toArrayList()
                }
            }

            Map::class, HashMap::class -> {
                assert(valueList.size == 1)
                val sepChar = param.findAnnotation<SepChar>()
                val map = HashMap<Any, Any>()
                for (pair in valueList.first().split(sepChar?.list ?: ',')) {
                    val p = pair.split(sepChar?.map ?: ':').map { it.trim() }
                    val k = stringToSingle(firstArgumentClass!!, p.first()) ?: continue
                    val v = stringToSingle(secondArgumentClass!!, p.second()) ?: continue
                    map[k] = v
                }
                return map
            }

            else -> {
                assert(valueList.size == 1)
                return stringToSingle(paramClass, valueList.first())
            }
        }
    }
}

fun stringToSingle(toClass: KClass<*>, fromValue: String): Any? {
    return when (toClass) {
        String::class -> fromValue
        Byte::class -> fromValue.toByteOrNull()
        Short::class -> fromValue.toShortOrNull()
        Int::class -> fromValue.toIntOrNull()
        Long::class -> fromValue.toLongOrNull()
        Float::class -> fromValue.toFloatOrNull()
        Double::class -> fromValue.toDoubleOrNull()
        Boolean::class -> fromValue.toBooleanValue()
        KsonObject::class -> Kson.parse(fromValue) as? KsonObject
        KsonArray::class -> Kson.parse(fromValue) as? KsonArray
        ConfigMap::class -> Configs.parse(fromValue) as? ConfigMap
        ConfigList::class -> Configs.parse(fromValue) as? ConfigList
        else -> error("不支持的参数类型: $toClass")
    }
}

val KType.firstGenericArgumentClass: KClass<*>? get() = this.genericArgs.firstOrNull()?.type?.classifier as? KClass<*>
val KType.secondGenericArgumentClass: KClass<*>? get() = this.genericArgs.secondOrNull()?.type?.classifier as? KClass<*>

private fun validateParam(param: KParameter, value: String): String {
    var strValue = value
    if (param.hasAnnotation<Trim>()) {
        strValue = strValue.trim()
    }
    val len = param.findAnnotation<Length>()
    if (len != null) {
        if (len.fixed != 0L && strValue.length.toLong() != len.fixed) {
            error("${param.name} 太长")
        }
        if (strValue.length < len.min) {
            error("${param.name} 太短")
        }
        if (len.max > 0 && strValue.length > len.max) {
            error("${param.name} 太长")
        }
    }
    return strValue
}