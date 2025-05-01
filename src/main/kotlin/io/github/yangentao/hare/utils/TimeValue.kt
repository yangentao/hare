package io.github.yangentao.hare.utils

import java.util.concurrent.TimeUnit

data class TimeValue(val unit: TimeUnit, val value: Long) {

    val toMilliSeconds: TimeValue get() = TimeValue(TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS.convert(value, unit))
    val toSeconds: TimeValue get() = TimeValue(TimeUnit.SECONDS, TimeUnit.SECONDS.convert(value, unit))
    val toMinutes: TimeValue get() = TimeValue(TimeUnit.MINUTES, TimeUnit.MINUTES.convert(value, unit))
    val toHours: TimeValue get() = TimeValue(TimeUnit.HOURS, TimeUnit.HOURS.convert(value, unit))
    val toDays: TimeValue get() = TimeValue(TimeUnit.DAYS, TimeUnit.DAYS.convert(value, unit))
    fun toTimeUnit(newTimeUnit: TimeUnit): TimeValue {
        return TimeValue(newTimeUnit, newTimeUnit.convert(value, unit))
    }

    companion object {
        fun milliseconds(value: Long): TimeValue {
            return TimeValue(TimeUnit.MILLISECONDS, value)
        }

        fun seconds(value: Long): TimeValue {
            return TimeValue(TimeUnit.SECONDS, value)
        }

        fun minutes(value: Long): TimeValue {
            return TimeValue(TimeUnit.MINUTES, value)
        }

        fun hours(value: Long): TimeValue {
            return TimeValue(TimeUnit.HOURS, value)
        }

        fun days(value: Long): TimeValue {
            return TimeValue(TimeUnit.DAYS, value)
        }
    }
}

val Number.timeMilliSeconds: TimeValue get() = TimeValue(TimeUnit.MILLISECONDS, this.toLong())
val Number.timeSeconds: TimeValue get() = TimeValue(TimeUnit.SECONDS, this.toLong())
val Number.timeMinutes: TimeValue get() = TimeValue(TimeUnit.MINUTES, this.toLong())
val Number.timeHours: TimeValue get() = TimeValue(TimeUnit.HOURS, this.toLong())
val Number.timeDays: TimeValue get() = TimeValue(TimeUnit.DAYS, this.toLong())


