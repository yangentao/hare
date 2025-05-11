package io.github.yangentao.hare.utils

import io.github.yangentao.types.TimeValue
import io.github.yangentao.xlog.loge
import java.util.concurrent.*

//java.specification.version 21
val javaVersionInt: Int = System.getProperty("java.specification.version")?.toString()?.toIntOrNull() ?: 0
fun startThreadTask(task: Runnable) {
    if (javaVersionInt >= 21) {
        Thread.ofVirtual().start(task)
    } else {
        Tasks.submit(task)
    }
}

fun startTask(task: Runnable) {
    if (javaVersionInt >= 21) {
        Thread.ofVirtual().start(task)
    } else {
        Tasks.submit(task)
    }
}

object Tasks : TaskPool(4)

@Suppress("UNUSED_PARAMETER")
fun uncaughtException(thread: Thread, ex: Throwable) {
    loge("uncaughtException: ", thread.name)
    loge(ex)
    ex.printStackTrace()
}

open class TaskPool(val corePoolSize: Int = 4) {
    val service: ScheduledExecutorService = Executors.newScheduledThreadPool(corePoolSize) {
        Thread(it, "TaskPool").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY
            setUncaughtExceptionHandler(::uncaughtException)
        }
    }

    fun close() {
        service.close()
    }

    //result = exec. submit(aCallable).get()
    fun <T> call(time: TimeValue, task: Callable<T>): ScheduledFuture<T> {
        return service.schedule(task, time.value, time.unit)
    }

    //result = exec. submit(aCallable).get()
    fun <T> call(task: Callable<T>): Future<T> {
        return service.submit(task)
    }

    fun submit(task: Runnable): Future<*> {
        return service.submit(task)
    }

    //TimeUnit.MILLISECONDS
    fun delayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
        return service.schedule(block, delay, TimeUnit.MILLISECONDS)
    }

    //TimeUnit.MILLISECONDS
    fun fixedDelayMill(delay: Long, block: Runnable): ScheduledFuture<*> {
        return service.scheduleWithFixedDelay(block, delay, delay, TimeUnit.MILLISECONDS)
    }

    //TimeUnit.MILLISECONDS
    fun fixedRateMill(period: Long, block: Runnable): ScheduledFuture<*> {
        return service.scheduleAtFixedRate(block, period, period, TimeUnit.MILLISECONDS)
    }

    fun delay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.schedule(block, delay.value, delay.unit)
    }

    fun fixedDelay(delay: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.scheduleWithFixedDelay(block, delay.value, delay.value, delay.unit)
    }

    fun fixedRate(period: TimeValue, block: Runnable): ScheduledFuture<*> {
        return service.scheduleAtFixedRate(block, period.value, period.value, period.unit)
    }

}

