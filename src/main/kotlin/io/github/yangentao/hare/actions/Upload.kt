@file:OptIn(ExperimentalUuidApi::class)
@file:Suppress("unused")

package io.github.yangentao.hare.actions


import io.github.yangentao.anno.ModelField
import io.github.yangentao.hare.HttpContext
import io.github.yangentao.hare.utils.FilePath
import io.github.yangentao.hare.utils.ensureDirs
import io.github.yangentao.hare.utils.md5Value
import io.github.yangentao.hare.utils.quiet
import io.github.yangentao.httpbasic.HttpFile
import io.github.yangentao.sql.TableModel
import io.github.yangentao.sql.TableModelClass
import io.github.yangentao.sql.update
import io.github.yangentao.types.DAY_MILLS
import io.github.yangentao.types.Now
import io.github.yangentao.xlog.loge
import java.io.File
import java.sql.Timestamp
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class Upload : TableModel() {

    //uuid
    @ModelField(primaryKey = true)
    var ident: String by model

    @ModelField(defaultValue = "")
    var subDir: String by model

    @ModelField
    var localName: String by model

    @ModelField
    var rawName: String by model

    @ModelField
    var mime: String by model

    @ModelField(index = true)
    var size: Long by model

    @ModelField(index = true)
    var md5: String? by model

    @ModelField
    var uploadTime: Timestamp by model

    //0: normal; 1:deleted
    @ModelField(index = true, defaultValue = "0")
    var state: Int by model

    @ModelField(index = true)
    var accountId: Long by model

    @ModelField(index = true, defaultValue = "0")
    var deadTime: Long by model

    val relatePath: String
        get() {
            if (subDir.isEmpty()) return localName
            return "$subDir/$localName"
        }

    fun localFile(base: File): File {
        return File(base, relatePath)
    }

    fun localFile(context: HttpContext): File {
        return File(context.dirUpload, relatePath)
    }

    fun toHttpFile(context: HttpContext): HttpFile {
        return HttpFile(localFile(context), filename = rawName, mime = mime)
    }

    companion object : TableModelClass<Upload>() {
        const val ST_NORMAL: Int = 0
        const val ST_DELETED: Int = 1

        fun markDeleted(ident: String) {
            val item = oneByKey(ident) ?: return
            item.update {
                it.state = ST_DELETED
            }
        }

        fun deleteWithFile(ident: String, baseDir: File) {
            val item = oneByKey(ident) ?: return
            item.localFile(baseDir).delete()
            item.deleteByKey()
        }

        fun fromHttpFile(httpFile: HttpFile, baseDir: File, subdir: String = "", accountId: Long = 0L, aliveDays: Int = 0): Upload? {
            var ext = FilePath.ext(httpFile.filename)
            if (ext.isEmpty()) ext = "tmp"
            val ident = Uuid.random().toHexString()
            val m = Upload()
            m.ident = ident
            m.subDir = subdir
            m.rawName = httpFile.filename
            m.localName = "$ident.$ext"
            m.mime = httpFile.mime // Mimes.ofExt(ext)
            m.size = httpFile.file.length()
            m.accountId = accountId
            m.uploadTime = Now.timestamp
            m.state = ST_NORMAL
            if (aliveDays > 0) {
                m.deadTime = Now.millis + aliveDays.DAY_MILLS
            }

            val localFile = m.localFile(baseDir)
            try {
                localFile.parentFile.ensureDirs()
                httpFile.file.renameTo(localFile)
                m.md5 = localFile.md5Value()
            } catch (ex: Exception) {
                quiet {
                    localFile.delete()
                    httpFile.file.delete()
                }
                loge(ex)
                return null
            }
            m.insert()
            return m
        }

    }
}

