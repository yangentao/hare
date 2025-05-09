@file:Suppress("unused", "FunctionName")

package io.github.yangentao.hare

import io.github.yangentao.anno.Name
import io.github.yangentao.hare.HttpRouter.Companion.NAME_TRIM_END
import io.github.yangentao.hare.actions.StaticDirectoryService
import io.github.yangentao.hare.log.logd
import io.github.yangentao.hare.log.loge
import io.github.yangentao.hare.utils.UriPath
import io.github.yangentao.hare.utils.firstTyped
import io.github.yangentao.hare.utils.ine
import io.github.yangentao.httpbasic.HttpMethod
import io.github.yangentao.types.isPublic
import io.github.yangentao.types.ownerObject
import io.github.yangentao.types.printX
import io.github.yangentao.types.putValue
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

// lambda 只能是 一个参数, 没有返回值. {(HttpContext)-> Unit  }
// 函数, 可以有返回值   fun hello(context:HttpContext):String {}
// 函数返回,  Unit=函数体内已经处理完了返回内容;  null=没有找到资源,会自动返回404; 其他值=调用ResultSender发送数据
// exactPath只有精确匹配时用到.
// HttpRouter(prefixPath="/app")
class HttpRouter(val prefixPath: String) {
    //uri别名, HttpContext.aliasUri
    internal val aliasList: ArrayList<UriAlias> = ArrayList()

    //精确匹配, lowercase
    private val _exactMap: HashMap<String, RouterAction> = HashMap()

    //模糊匹配
    private val matchList: ArrayList<RouterAction> = ArrayList()
    private val beforeList: ArrayList<RouterInterceptor> = ArrayList()
    private val afterList: ArrayList<RouterInterceptor> = ArrayList()
    private val beforeAllList: ArrayList<RouterInterceptor> = ArrayList()
    private val afterAllList: ArrayList<RouterInterceptor> = ArrayList()
    private val senderList: ArrayList<ResultSender> = arrayListOf(DefaultResultSender())

    private var staticService: StaticDirectoryService? = null
    private var staticServiceAction: RouterAction? = null

    fun routePathOf(requestUri: String): String? {
        return UriPath(requestUri).trimStartPath(prefixPath)
    }

    fun staticService(prefix: String, dir: File) {
        val ss = StaticDirectoryService(prefix, dir)
        staticServiceAction = RouterAction(ss.matcher, ss::service)
        staticService = ss
    }

    fun aliasOf(routePath: String): String? {
        for (a in aliasList) {
            val newUri = a.aliasOf(routePath) ?: continue
            if (newUri ine routePath) return newUri
        }
        return null
    }

    fun addAlias(uriAlias: UriAlias) {
        aliasList += uriAlias
    }

    private fun addRoute(ra: RouterAction) {
        logd("Route: ", ra.match, ra.action)
        if (ra.match is EqualMatch) {
            val old = _exactMap.put(ra.match.path, ra)
            if (old != null) {
                error("路由已存在: path: ${ra.match.path}, old: $old, new: ${ra.action} ")
            }
        } else {
            matchList.add(ra)
        }
    }

    fun addMatch(match: UriMatch, action: KFunction<*>) {
        checkInstanceError(action)
        addRoute(RouterAction(match, action))
    }

    fun beforeAll(interceptor: KFunction<Unit>) {
        checkInstanceError(interceptor)
        val exist = beforeAllList.any { it.action === interceptor }
        if (!exist) beforeAllList += RouterInterceptor(interceptor)
    }

    fun afterAll(interceptor: KFunction<Unit>) {
        checkInstanceError(interceptor)
        val exist = afterAllList.any { it.action === interceptor }
        if (!exist) afterAllList += RouterInterceptor(interceptor)
    }

    fun beforeFunc(func: (HttpContext) -> Unit) {
        val exist = beforeList.any { it.action === func }
        if (!exist) beforeList += RouterInterceptor(func)
    }

    fun beforeActions(vararg interceptors: KFunction<Unit>) {
        interceptors.forEach { beforeAction(it) }
    }

    fun beforeAction(interceptor: KFunction<Unit>) {
        checkInstanceError(interceptor)
        val exist = beforeList.any { it.action === interceptor }
        if (!exist) beforeList += RouterInterceptor(interceptor)
    }

    fun afterFunc(func: (HttpContext) -> Unit) {
        val exist = afterList.any { it.action === func }
        if (!exist) afterList += RouterInterceptor(func)
    }

    fun afterActions(vararg interceptors: KFunction<Unit>) {
        interceptors.forEach { afterAction(it) }
    }

    fun afterAction(interceptor: KFunction<Unit>) {
        checkInstanceError(interceptor)
        val exist = afterList.any { it.action === interceptor }
        if (!exist) afterList += RouterInterceptor(interceptor)
    }

    fun pushSender(resultSender: ResultSender) {
        senderList.add(0, resultSender)
    }

    fun findAction(routePath: String): RouterAction? {
        _exactMap[routePath.lowercase()]?.also { return it }
        matchList.filter { it.match !is PrefixUriMatch }.firstOrNull { it.match.match(routePath) }?.also { return it }
        matchList.filter { it.match is PrefixUriMatch }.sortedByDescending { (it.match as PrefixUriMatch).prefixUri.length }.firstOrNull { it.match.match(routePath) }?.also { return it }
        staticServiceAction?.also { if (it.match.match(routePath)) return it }
        return null
    }

    fun invokeRouterAction(context: HttpContext, action: RouterAction) {
        try {
            beforeAllList.forEach { it.invoke(context) }
            if (context.commited) {
                return
            }
            beforeList.forEach { it.invoke(context, mapOf(action::class to action)) }
            if (!context.commited) invokeAction(context, action)
            afterList.forEach { it.invoke(context, mapOf(action::class to action)) }
            if (!context.commited) {
                loge("NOT commited: ", context.requestUri)
            }
        } catch (e: Throwable) {
            context.app.error(context, e)
        } finally {
            afterAllList.forEach { it.invoke(context) }
            context.doClean()
        }
    }

    private fun invokeAction(context: HttpContext, action: RouterAction) {
        val result = action.invoke(context)
        if (context.commited) return
//        if (!action.isKFunction) return

        when (result) {
            null -> context.sendError404()
            Unit -> return
            is java.util.concurrent.Future<*> -> return
            else -> {
                for (r in senderList) {
                    if (r.sendResult(context, action, result)) return
                }
            }
        }
        if (!context.commited) error("No sender found: \n $result, \n $action")
    }

    private val isPatternReg: Regex = Regex(""".*\{.+}.*""")

    private fun checkInstanceError(action: KFunction<*>) {
        if (action.ownerObject === this) error("添加的Action $action 的实例, 是HttpRouter!")
    }

    //lambda or kfunction
    fun addFunc(path: String, action: LambdaAction) {
        addRoute(RouterAction(EqualMatch(path), action))
    }

    fun addActions(vararg actions: KFunction<*>) {
        for (a in actions) addAction(a)
    }

    //addAction("say", ::hello)
    fun addAction(path: String, action: KFunction<*>) {
        checkInstanceError(action)
        val anno = action.findAnnotation<Action>()
        val at = anno?.type ?: PathType.normal
        when (at) {
            PathType.normal -> addRoute(RouterAction(EqualMatch(path), action))
            PathType.prefix -> addRoute(RouterAction(PrefixUriMatch(path), action))
            PathType.regex -> addRoute(RouterAction(RegexUriMatch(path), action))
            PathType.pattern -> {
                val pattern: Boolean = path.matches(isPatternReg)
                if (!pattern) error("type is pattern , but path is not a pattern, like 'hello/{id}'.")
                addRoute(RouterAction(PatternUriMatch(path, true), action))
            }
        }
    }

    //addAction(::hello)
    fun addAction(action: KFunction<*>) {
        checkInstanceError(action)
        val path = action.routePath()
        addAction(path, action)
    }

    //addGroup(Dev::class)
    fun addGroups(vararg groups: KClass<*>) {
        for (g in groups) addGroup(g)
    }

    fun addGroup(group: KClass<*>) {

        val groupName: String = group.routePath()

        val pubList: List<KFunction<*>> = group.memberFunctions.filter { it.isPublic }
        val acList: List<KFunction<*>> = pubList.filter { it.hasAnnotation<Action>() }
        val beList: List<KFunction<*>> = pubList.filter { it.hasAnnotation<BeforeAction>() && !it.hasAnnotation<Action>() }
        val afList: List<KFunction<*>> = pubList.filter { it.hasAnnotation<AfterAction>() && !it.hasAnnotation<Action>() }
        for (f in acList) {
            val anno: Action = f.findAnnotation()!!
            if (anno.index) {
                addRoute(RouterAction(EqualMatch(groupName), f, beList, afList, group))
                continue
            }
            val actionName: String = f.routePath()
            val path: String = UriPath.join(groupName, actionName).lowercase()
            when (anno.type) {
                PathType.normal -> addRoute(RouterAction(EqualMatch(path), f, beList, afList, group))
                PathType.prefix -> addRoute(RouterAction(PrefixUriMatch(path), f, beList, afList, group))
                PathType.regex -> addRoute(RouterAction(RegexUriMatch(path), f, beList, afList, group))
                PathType.pattern -> {
                    val pattern: Boolean = actionName.isNotEmpty() && anno.path.matches(isPatternReg)
                    if (!pattern) error("type is pattern , but path is not a pattern, like 'hello/{id}'.")
                    addRoute(RouterAction(PatternUriMatch(path, true), f, beList, afList, group))
                }
            }
        }
    }

    fun dumpRouters() {
        printX("Routes: $prefixPath")
        for (e in _exactMap.entries) {
            if (e.key.isEmpty()) {
                printX("[index] :", e.value)
            } else {
                printX(e.key, ":", e.value)
            }
        }
        println()
        for (e in matchList) {
            printX(e.match, " action:", e.action)
        }
        printX()
    }

    companion object {
        //大小写敏感, 以此结尾的函数或类, 在注册到路由时, 被trim掉
        // class UserActions  => user
        // fun uploadAction  => upload
        val NAME_TRIM_END: HashSet<String> = hashSetOf("Action", "Actions", "Page", "Controller", "Group")
    }

}

class TargetRouterAction(val router: HttpRouter, val routePath: UriPath, val action: RouterAction) {
    fun checkMethods(method: String): Boolean {
        val ls = action.methods
        if (method in ls) return true
        if (method == HttpMethod.HEAD && HttpMethod.GET in ls) return true
        return false
    }

    fun process(context: HttpContext) {
        val m = action.match
        if (m is PatternUriMatch) {
            val map = m.matchResult(routePath.value)
            if (map != null) {
                for ((k, v) in map) {
                    context.paramMap.putValue(k, v)
                }
            }
        }
        router.invokeRouterAction(context, action)
    }

    companion object {
        val defaultMethods = listOf(HttpMethod.GET, HttpMethod.POST)
    }
}

private fun KFunction<*>.routePath(): String {
    val name = this.name
    if (name.isEmpty() || name.startsWith('<')) error("没有名字: $this ")
    return makeRouterPath(name, this.annotations)
}

private fun KClass<*>.routePath(): String {
    this.findAnnotation<Action>()
    val name = this.simpleName!!
    if (name.isEmpty()) error("addGroup添加的类, 没有名字: $this")
    return makeRouterPath(name, this.annotations)
}

private fun makeRouterPath(name: String, annotations: List<Annotation>): String {

    annotations.firstTyped<Action>()?.also { ac ->
        if (ac.index) return ""
        if (ac.path.isNotEmpty()) return ac.path.lowercase()
    }
    annotations.firstTyped<Name>()?.also { na ->
        return na.value.lowercase()
    }
    for (a in NAME_TRIM_END) {
        if (name.length > a.length && name.endsWith(a)) return name.substringBeforeLast(a).lowercase()
    }
    return name.lowercase()
}

