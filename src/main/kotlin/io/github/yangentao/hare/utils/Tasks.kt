package io.github.yangentao.hare.utils

import io.github.yangentao.types.Tasks

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
