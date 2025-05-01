package io.github.yangentao.hare.utils


import java.io.File
import kotlin.reflect.KClass

@Throws(IllegalArgumentException::class)
fun findAppWorkDirectory(args: Array<String>, workName: String = "work"): File {
    val commandLine: CmdLine = CmdLine(args)
    val argWork = commandLine.get(workName)
    if (argWork != null && argWork.isNotEmpty()) {
        val f = File(argWork)
        if (!f.isDirectory) {
            f.mkdirs()
            if (f.isDirectory) {
                return f
            }
        }
        error("Work dir special, but create directory fail")
    }

    val file = MainClass.find().location
    if (file.isFile) {
        // app{xxx.exe(jar), work}
        val pd = file.parentFile
        val pdw = File(pd, workName)
        if (pdw.isDirectory) {
            return pd
        }
        // app/{bin, lib, work, ...}
        val ppd = pd.parentFile
        val a = File(ppd, "bin").isDirectory
        val b = File(ppd, "lib").isDirectory
        if (a && b) {
            val w = File(ppd, workName)
            if (w.isDirectory) {
                return w
            }
            w.mkdir()
            if (w.isDirectory) {
                return w
            }
            error("create work directory fail")
        }
        error("No work diretory found!")
    } else {
        // debug run , project/work
        val debugSufix = "/build/classes/kotlin/main"
        val p = file.canonicalPath
        if (p.endsWith(debugSufix)) {
            val f = File(p.substring(0, p.length - debugSufix.length), "work")
            if (f.isDirectory) {
                return f
            }
        }
    }
    error("No work diretory found!")
}

//build/classes/kotlin/main   OR xxx.jar
val KClass<*>.location: File
    get() {
        return this.java.location
    }
val Class<*>.location: File
    get() {
        return File(this.protectionDomain.codeSource.location.path.decodedURL)
    }

object MainClass {
    private var mainClass: Class<*>? = null

    // in main thread!
    fun find(): Class<*> {
        if (mainClass != null) return mainClass!!
        val th = Thread.currentThread()
        assert(th.isMain)
        val st = th.stackTrace.last()
        assert(st.methodName == "main")
        val c = Class.forName(st.className)
        mainClass = c
        return c
    }
}