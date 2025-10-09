package io.github.yangentao.hare

import io.github.yangentao.types.DateTime
import io.github.yangentao.types.MIN_MILLS
import io.github.yangentao.types.safe
import java.util.concurrent.ScheduledFuture

abstract class AppTask(val app: HttpApp) : Runnable {
    protected var fu: ScheduledFuture<*>? = null

    abstract fun onAttach()
    open fun onDetach() {
        fu?.cancel(false)
        fu = null
    }

    open fun cancel() {
        fu?.cancel(false)
        fu = null
    }
}

abstract class DelayAppTask(app: HttpApp, val delay: Long) : AppTask(app) {

    override fun onAttach() {
        fu = app.taskPool.delayMill(delay, this)
    }

}

abstract class FixRateAppTask(app: HttpApp, val delay: Long) : AppTask(app) {
    override fun onAttach() {
        fu = app.taskPool.fixedRateMill(delay, this)
    }
}

abstract class FixDelayAppTask(app: HttpApp, val delay: Long) : AppTask(app) {
    override fun onAttach() {
        fu = app.taskPool.fixedDelayMill(delay, this)
    }

}

class EveryMinuteTask(app: HttpApp) : AppTask(app) {
    private var preHour: Int = -1
    private var preMin: Int = -1
    private val tasks: ArrayList<HourMinuteTask> = ArrayList()

    fun add(task: HourMinuteTask) {
        tasks.add(task)
        task.onAttach()
    }

    fun remove(task: HourMinuteTask) {
        tasks.remove(task)
        task.onDetach()
    }

    override fun onAttach() {
        fu = app.taskPool.fixedRateMill(1.MIN_MILLS, this)
    }

    override fun onDetach() {
        super.onDetach()
        for (t in tasks) {
            safe {
                t.onDetach()
            }
        }
        tasks.clear()
    }

    override fun run() {
        val tm = DateTime.now
        val h = tm.hour
        val m = tm.minute
        if (h != preHour || m != preMin) {
            preHour = h
            preMin = m
            onMinute(h, m)
        }
    }

    private fun onMinute(hour: Int, minute: Int) {
        val ls = tasks.filter { (it.hour == hour || it.hour == -1) && it.minute == minute }
        for (t in ls) {
            safe {
                t.run()
            }
        }
    }
}

abstract class HourMinuteTask(val app: HttpApp, val hour: Int, val minute: Int) : Runnable {
    abstract fun onAttach()
    open fun onDetach() {}
}