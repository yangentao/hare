@file:Suppress("unused")

package io.github.yangentao.hare

import io.github.yangentao.config.ConfigMap
import io.github.yangentao.config.Configs
import io.github.yangentao.hare.utils.UriPath
import io.github.yangentao.hare.utils.ensureDirs
import io.github.yangentao.hare.utils.joinPath
import io.github.yangentao.kson.JsonFailed
import io.github.yangentao.sql.TableMigrater
import io.github.yangentao.sql.TableModel
import io.github.yangentao.types.*
import io.github.yangentao.xlog.*
import java.io.File
import kotlin.reflect.KClass

/**
 * contextPath, start with '/',   "/web"
 */
@Suppress("CanBeParameter")
class HttpApp(
    contextPath: String, val name: String, val work: File,
    val dirWeb: File = File(work, "web").ensureDirs(),
    val dirData: File = File(work, "data").ensureDirs(),
    val dirUpload: File = File(work, "upload").ensureDirs(),
    val dirConfig: File = File(work, "config").ensureDirs(),
    val dirLog: File = File(work, "log").ensureDirs()
) {
    val contextPath: String = if (contextPath.startsWith('/')) contextPath else "/$contextPath"

    val routers: ArrayList<HttpRouter> = ArrayList()

    var onDestory: (() -> Unit)? = null
    var onError: ((HttpContext, Throwable) -> Boolean)? = null
    var onAuthFailed: ((HttpContext) -> Boolean)? = null

    val attrStore = AttrStore()
    val taskPool = TaskPool()

    val cleanList: ArrayList<AppCleaner> = ArrayList()
    val taskList: ArrayList<AppTask> = ArrayList()
    val everyMinuteTask: EveryMinuteTask = EveryMinuteTask(this)

    init {
        val dp = DirPrinter(dirLog, fileSize = MB * 10L, maxDays = 30)
        val ep = DirPrinter(dirLog, fileSize = MB * 10L, maxDays = 30, baseName = "err")
        XLog.setPrinter(
            TreePrinter(
                ConsolePrinter,
                FilterPrinter(dp, LevelFilter(LogLevel.DEBUG)),
                FilterPrinter(ep, LevelFilter(LogLevel.ERROR)),
            )
        )
        addTask(everyMinuteTask)
    }

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
        taskPool.close()
        for (t in taskList) {
            safe {
                t.onDetach()
            }
        }
        for (c in cleanList) {
            safe {
                c.run()
            }
        }
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

    fun every(time: TimeValue, block: () -> Unit) {
        taskPool.fixedDelay(time, block)
    }

    fun migrate(vararg clses: KClass<out TableModel>) {
        TableMigrater.migrate(*clses)
    }

    fun loadConfig(): ConfigMap? {
        val configFile: File = File(dirConfig, "config.txt")
        if (configFile.exists()) {
            return Configs.parseFile(configFile) as? ConfigMap
        }
        return null
    }

    fun addCleaner(cleaner: AppCleaner) {
        this.cleanList += cleaner
    }

    fun removeCleaner(cleaner: AppCleaner) {
        this.cleanList.remove(cleaner)
    }

    fun addTask(task: AppTask) {
        taskList += task
        task.onAttach()
    }

    fun removeTask(task: AppTask) {
        taskList.remove(task)
        task.onDetach()
    }

    fun addHourMinuteTask(task: HourMinuteTask) {
        everyMinuteTask.add(task)
    }

    fun removeHourMinuteTask(task: HourMinuteTask) {
        everyMinuteTask.remove(task)
    }

    fun atTime(hour: Int, minute: Int, callback: Runnable) {
        addHourMinuteTask(HourMinuteTask(hour, minute, callback))
    }

    fun atHour(hour: Int, callback: Runnable) {
        addHourMinuteTask(HourTask(hour, callback))
    }

    fun atMinute(minute: Int, callback: Runnable) {
        addHourMinuteTask(MinuteTask(minute, callback))
    }
}

@DslMarker
annotation class AppMarker

interface AppCleaner : Runnable {

}