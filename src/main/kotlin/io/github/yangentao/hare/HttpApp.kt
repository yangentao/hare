@file:Suppress("unused")

package io.github.yangentao.hare

import io.github.yangentao.config.ConfigMap
import io.github.yangentao.config.Configs
import io.github.yangentao.hare.log.configLog4J
import io.github.yangentao.hare.log.loge
import io.github.yangentao.hare.log.setGlobalLogger
import io.github.yangentao.hare.utils.AttrStore
import io.github.yangentao.hare.utils.Tasks
import io.github.yangentao.hare.utils.UriPath
import io.github.yangentao.hare.utils.ensureDirs
import io.github.yangentao.hare.utils.joinPath
import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.sql.TableMigrater
import io.github.yangentao.sql.TableModel
import io.github.yangentao.types.TimeValue
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.reflect.KClass

class HttpApp(
    val contextPath: String, val name: String, val work: File,
    val dirWeb: File = File(work, "web").ensureDirs(),
    val dirData: File = File(work, "data").ensureDirs(),
    val dirUpload: File = File(work, "upload").ensureDirs(),
    val dirConfig: File = File(work, "config").ensureDirs(),
    val dirLog: File = File(work, "log").ensureDirs()
) {
    val routers: ArrayList<HttpRouter> = ArrayList()
    val webSockets: LinkedHashMap<String, LinkedHashMap<String, KClass<*>>> = LinkedHashMap()

    var onDestory: (() -> Unit)? = null
    var onError: ((HttpContext, Throwable) -> Boolean)? = null
    var onAuthFailed: ((HttpContext) -> Boolean)? = null

    val attrStore = AttrStore()

//    val addr: String? by attrStore

    fun findRouter(requestURI: String): TargetRouterAction? {
        for (r in routers) {
            val p = UriPath(requestURI).trimStartPath(r.prefixPath) ?: continue
            val s = r.aliasOf(p)
            val pp = UriPath(s ?: p)
            r.findAction(pp.value)?.also { ac -> return TargetRouterAction(r, pp, ac) }
        }
        return null
    }

    fun authFailed(context: HttpContext) {
        if (true == onAuthFailed?.invoke(context)) return
    }

    fun destroy() {
        onDestory?.invoke()
    }

    fun error(context: HttpContext, e: Throwable) {
        loge(e)
        if (true == onError?.invoke(context, e)) return
        when (e) {
            is NetClientError -> {
                if (e.result != null) {
                    context.sendJson(e.result.toString())
                } else {
                    context.sendJson(JsonFailed(e.message ?: "client error").toString())
                }
            }

            is NetServerError -> {
                e.printStackTrace()
                context.sendError500(e)
                loge(e)
            }

            else -> {
                e.printStackTrace()
                context.sendError500(e)
            }
        }

    }

    fun configLog(
        console: Boolean = true,
        levelConsole: Level = Level.DEBUG,
        levelFile: Level = Level.INFO,
        levelMine: Level = Level.DEBUG,
        levelRoot: Level = Level.INFO,
        minePackage: String = "dev.entao",
    ) {
        configLog4J(dirLog, console = console, levelConsole = levelConsole, levelMine = levelMine, levelRoot = levelRoot, minePackage = minePackage)
        setGlobalLogger(LogManager.getLogger())
    }

    fun router(block: RouterConfig.() -> Unit) {
        val r = RouterConfig(HttpRouter(contextPath))
        routers.add(r.httpRouter)
        r.block()
    }

    fun router(path: String, block: RouterConfig.() -> Unit) {
        val r = RouterConfig(HttpRouter(joinPath(contextPath, path)))
        routers.add(r.httpRouter)
        r.block()
    }

    fun websocket(uri: String, path: String, cls: KClass<*>) {
        val m = webSockets.getOrPut(uri) { LinkedHashMap() }
        m[path] = cls
    }

    fun websocket(uri: String, block: WebSocketConfig.() -> Unit) {
        val c = WebSocketConfig(uri).apply(block)
        val m = webSockets.getOrPut(uri) { LinkedHashMap() }
        for ((k, v) in c.endpoints) {
            m[k] = v
        }
    }

    fun every(time: TimeValue, block: () -> Unit) {
        Tasks.fixedDelay(time, block)
    }

    fun migrate(vararg clses: KClass<out TableModel>) {
        TableMigrater.migrate(*clses)
    }

    fun loadConfig(): ConfigMap? {
        val configFile: File = File(dirConfig, "config.txt")
        if (configFile.exists()) {
            return Configs.parseFile(configFile).asMap
        }
        return null
    }
}

class WebSocketConfig(val uri: String, val endpoints: LinkedHashMap<String, KClass<*>> = LinkedHashMap()) {
    fun endpoint(path: String, cls: KClass<*>) {
        endpoints[path] = cls
    }
}

@DslMarker
annotation class AppMarker