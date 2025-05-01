@file:Suppress("unused")

package io.github.yangentao.hare


import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

@AppMarker
class RouterConfig(val httpRouter: HttpRouter) {
    fun rename(fromUri: String, toUri: String) {
        httpRouter.addAlias(RenameAlias(fromUri, toUri))
    }

    fun renamePath(fromUri: String, toUri: String) {
        httpRouter.addAlias(PrefixAlias(fromUri, toUri))
    }

    fun alias(fromUri: Regex, toUri: String) {
        httpRouter.addAlias(RegexAlias(fromUri, toUri))
    }

    fun webroot(prefix: String, dir: File) {
        httpRouter.staticService(prefix, dir)
    }

    fun prefix(prefix: String, action: KFunction<*>) {
        httpRouter.addMatch(PrefixUriMatch(prefix), action)
    }

    // "/delete/{ident}/{name}"
    fun pattern(pattern: String, action: KFunction<*>) {
        httpRouter.addMatch(PatternUriMatch(pattern), action)
    }

    fun match(regex: Regex, action: KFunction<*>) {
        httpRouter.addMatch(RegexUriMatch(regex), action)
    }

    fun group(vararg classes: KClass<*>) {
        httpRouter.addGroups(*classes)
    }

    fun action(path: String, action: LambdaAction) {
        httpRouter.addFunc(path, action)
    }

    fun action(vararg funcs: KFunction<*>) {
        funcs.forEach { httpRouter.addAction(it) }
    }

    fun before(vararg funcs: KFunction<Unit>) {
        httpRouter.beforeActions(*funcs)
    }

    fun after(vararg funcs: KFunction<Unit>) {
        httpRouter.afterActions(*funcs)
    }

    fun beforeAll(vararg funcs: KFunction<Unit>) {
        funcs.forEach { httpRouter.beforeAll(it) }

    }

    fun afterAll(vararg funcs: KFunction<Unit>) {
        funcs.forEach { httpRouter.afterAll(it) }
    }
}