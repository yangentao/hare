@file:Suppress("unused")

package io.github.yangentao.hare.log

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

//private var Log: Logger = LogManager.getLogger("global")
private var Log: Logger = LogManager.getLogger()

fun setGlobalLogger(logger: Logger) {
    Log = logger
}

fun logd(vararg args: Any?) {
    val s = anyArrayToString(args)
    Log.debug(s)
}

fun logi(vararg args: Any?) {
    Log.info(anyArrayToString(args))
}

fun logw(vararg args: Any?) {
    Log.warn(anyArrayToString(args))
}

fun loge(vararg args: Any?) {
    Log.error(anyArrayToString(args))
}

fun fatal(msg: String, vararg args: Any?): Nothing {
    Log.fatal(msg)
    Log.fatal(anyArrayToString(args))
    error(msg)
}

fun fatalIf(b: Boolean?, msg: String, vararg args: Any?) {
    if (b == null || b) {
        Log.fatal(anyArrayToString(args))
        error(msg)
    }
}
