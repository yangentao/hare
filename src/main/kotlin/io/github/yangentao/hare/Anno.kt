package io.github.yangentao.hare

/**
 * 通用注释
 * Created by yangentao on 2016/12/14.
 */

@Suppress("EnumEntryName")
enum class PathType {
    normal, pattern, prefix, regex
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BeforeAction

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AfterAction

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Action(val path: String = "", val type: PathType = PathType.normal, val index: Boolean = false, val methods: Array<String> = [], val mime: String = "", val charset: String = "")





