package io.github.yangentao.hare.utils

import io.github.yangentao.xlog.loge

class HareThread(val callback: () -> Unit, val onError: ((Throwable) -> Unit)? = null, name: String? = null) : Thread(name ?: "hare-thread") {
    init {
        isDaemon = true
        priority = NORM_PRIORITY
        setUncaughtExceptionHandler(::hareUncaughtException)
    }

    override fun run() {
        try {
            callback()
        } catch (ex: Throwable) {
            onUncaughtException(ex)
        }
    }

    fun onUncaughtException(ex: Throwable) {
        if (onError != null) {
            onError.invoke(ex)
        } else {
            loge("uncaughtException: ", this.name)
            loge(ex)
            ex.printStackTrace()
        }
    }

}

private fun hareUncaughtException(thread: Thread, ex: Throwable) {
    if (thread is HareThread) {
        thread.onUncaughtException(ex)
    } else {
        loge("uncaughtException: ", thread.name)
        loge(ex)
        ex.printStackTrace()
    }
}