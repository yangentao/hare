@file:Suppress("unused")

package io.github.yangentao.hare.utils

import io.github.yangentao.hare.HttpContext
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation

annotation class OnLocale(val language: String, val country: String = "", val variant: String = "")

@Suppress("DEPRECATION")
val OnLocale.locale: Locale get() = Locale(language, country, variant)

/**
 * fun main() {
 *     val b = LocaleStrings<LangAbs>(listOf(LangZhCn::class, LangZh::class, LangEn::class))
 *     val s = b.of(context).hello
 * }
 * interface LangAbs {
 *     val hello: String
 * }
 *
 * @OnLocale("en")
 * open class LangEn : LangAbs {
 *     override val hello: String = "Hello"
 * }
 *
 * @OnLocale("zh")
 * open class LangZh : LangAbs {
 *     override val hello: String = "妳好"
 * }
 *
 * @OnLocale("zh", "CN")
 * class LangZhCn : LangZh() {
 *     override val hello: String = "你好"
 * }
 */
class LocaleStrings<T : Any>(private val languages: List<KClass<out T>>, private val defaultLanguage: KClass<out T> = languages.first()) {
    private val defLang: T by lazy { defaultLanguage.inst() }
    private val localeLangList: List<Pair<Locale, KClass<out T>>> = languages.map {
        it.findAnnotation<OnLocale>()!!.locale to it
    }

    private fun KClass<out T>.inst(): T {
        return this.objectInstance ?: this.createInstance()
    }

    fun of(context: HttpContext): T {
        val al = context.requestHeader("Accept-Language") ?: return defLang
        val list = parseAcceptLanguage(al)
        return find(list)
    }

    fun find(list: List<Locale>): T {
        if (list.isEmpty()) return defLang
        for (l in list) {
            for (p in localeLangList) {
                if (p.first.language == l.language) {
                    if (p.first.country == l.country) {

                        return p.second.inst()
                    }
                }
            }
        }
        for (l in list) {
            for (p in localeLangList) {
                if (p.first.language == l.language) {
                    if (p.first.country.isEmpty()) {
                        return p.second.inst()
                    }
                }
            }
        }
        return defLang
    }

    @Suppress("DEPRECATION")
    private fun parseAcceptLanguage(al: String): List<Locale> {
        val ls = al.split(',').map { it.trim() }
        val plist: ArrayList<Pair<Locale, Double>> = ArrayList<Pair<Locale, Double>>()
        for (item in ls) {
            val ql = item.split(';')
            val l = ql.firstOrNull()?.trim() ?: continue
            if (l.isEmpty() || l == "*") continue
            val q: Double = ql.secondOrNull()?.substringAfter('=')?.toDoubleOrNull() ?: 1.0
            val loc = Locale(l.substringBefore('-'), l.substringAfter('-', "").substringBefore('-'))
            plist.add(loc to q)
        }
        plist.sortByDescending { it.second }
        return plist.map { it.first }
    }
}


