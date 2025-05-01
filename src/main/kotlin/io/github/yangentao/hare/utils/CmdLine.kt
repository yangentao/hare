package io.github.yangentao.hare.utils

class CmdLine(args: Array<String>, prefix: List<String> = listOf("--", "-D", "-"), sepChars: List<Char> = listOf('='), private val ignoreCase: Boolean = true) {
    val items: List<ArgumentItem> = ArgumentItem.parse(args, prefix, sepChars)

    fun get(argument: String): String? {
        return items.firstOrNull { it.argument.equals(argument, ignoreCase) }?.value
    }

    @Suppress("unused")
    fun has(argument: String): Boolean {
        return items.firstOrNull { it.argument.equals(argument, ignoreCase) } != null
    }
}

data class ArgumentItem(val rawArgument: String, val argument: String, val value: String?) {
    val prefixTrimed: Boolean = rawArgument != argument

    override fun toString(): String {
        return if (prefixTrimed) {
            if (value == null) {
                "Arg{ $argument}"
            } else {
                "Arg{ $argument = $value}"
            }
        } else {
            "Arg{ $argument}"
        }
    }

    companion object {
        // -DHOME=/Users/entao/gitee/netserver/app/src/dist
        fun parse(args: Array<String>, prefix: List<String> = listOf("--", "-D", "-"), sepChars: List<Char> = listOf('=')): List<ArgumentItem> {
            val prefixed: List<String> = prefix.sortedByDescending { it.length }
            fun trimKey(key: String): String {
                for (p in prefixed) {
                    if (key.startsWith(p) && key.length > p.length) {
                        return key.substring(p.length)
                    }
                }
                return key
            }

            fun sepArg(arg: String): ArgumentItem {
                for (ch in sepChars) {
                    val idx = arg.indexOf(ch)
                    if (idx > 0) {
                        val key = arg.substring(0, idx).trim()
                        val value = arg.substring(idx + 1).trim()
                        return ArgumentItem(arg, trimKey(key), value)
                    }
                }
                return ArgumentItem(arg, trimKey(arg), null)
            }
            return args.map { sepArg(it) }
        }
    }
}
