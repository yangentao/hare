package io.github.yangentao.hare.feature

import io.github.yangentao.hare.ContextAttribute
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.RouterAction
import io.github.yangentao.hare.utils.firstTyped

//检查是否登录
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginNeed(val types: Array<String> = ["*"])

private const val TOKEN = "token"

inline fun <reified T : Any> T?.ifNotNull(block: (T) -> Unit): T? {
    if (this != null) block(this)
    return this
}

val HttpContext.tokenValue: String?
    get() {
        this.param(TOKEN)?.ifNotNull { return it }
        this.param("access_token")?.ifNotNull { return it }
        return requestHeader("Authorization")?.substringAfter("Bearer ", "")?.trim()
    }
var HttpContext.accountID: Long? by ContextAttribute
val HttpContext.accID: Long get() = accountID!!
var HttpContext.accountType: String? by ContextAttribute

fun isLoginTypeMatch(anno: LoginNeed, type: String): Boolean {
    return anno.types.any { it == "*" || it == type }
}

val platformList: List<String> = listOf("app", "mobile", "desktop", "web")

class TokenCheck {
    //SessionTable, 不是http的sessionID
    private fun checkSession(loginAnno: LoginNeed, context: HttpContext): Boolean {
        val token = context.tokenValue ?: return false
        val info = SessionTable.check(token) ?: return false
        if (info.expired) {
            SessionTable.remove(token)
            return false
        }
        if (!isLoginTypeMatch(loginAnno, info.type)) return false
        context.accountID = info.accId
        context.accountType = info.type
        return true
    }

    private fun checkToken(loginAnno: LoginNeed, context: HttpContext): Boolean {
        val token = context.tokenValue ?: return false
        val info = TokenTable.checkToken(token) ?: return false
        if (info.expired) {
            TokenTable.removeToken(token)
            return false
        }
        if (!isLoginTypeMatch(loginAnno, info.type)) return false
        context.accountID = info.id
        context.accountType = info.type
        return true
    }

    fun beforeAction(context: HttpContext, action: RouterAction) {
        if (!action.isKFunction) return
        val tokenAnno: LoginNeed = action.actionAnnotations.firstTyped() ?: action.groupAnnotations.firstTyped() ?: return
        if (checkSession(tokenAnno, context)) return
        if (checkToken(tokenAnno, context)) return
        context.onAuthFailed(action)
    }

    fun checkSessionAction(context: HttpContext, action: RouterAction) {
        if (!action.isKFunction) return
        val tokenAnno: LoginNeed = action.actionAnnotations.firstTyped() ?: action.groupAnnotations.firstTyped() ?: return
        if (checkSession(tokenAnno, context)) return
        context.onAuthFailed(action)
    }
}
