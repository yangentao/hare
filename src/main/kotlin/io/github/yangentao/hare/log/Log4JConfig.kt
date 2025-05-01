package io.github.yangentao.hare.log

import io.github.yangentao.hare.utils.ensureDirs
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Appender
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.core.appender.RollingFileAppender
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.filter.LevelRangeFilter
import org.apache.logging.log4j.core.layout.PatternLayout
import java.io.File

@Suppress("unused")
fun configLog4J(
    dir: File?,
    console: Boolean = true,
    levelConsole: Level = Level.DEBUG,
    levelFile: Level = Level.INFO,
    levelMine: Level = Level.DEBUG,
    levelRoot: Level = Level.INFO,
    minePackage: String = "dev.entao",
    singleFileSize: String = "10 MB",
    maxFileCount: Int = 300,
    baseName: String = "app"
) {
    if (dir == null && !console) return
    dir?.ensureDirs()
    val ctx: LoggerContext = LogManager.getContext(false) as LoggerContext
    val config: Configuration = ctx.configuration
    config.removeLogger(minePackage)
    config.removeLogger(LoggerConfig.ROOT)

    val layout: PatternLayout = PatternLayout.newBuilder()
        .withCharset(Charsets.UTF_8)
        .withConfiguration(config)
        .withPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%T %t] %-5level %c{1} : %m%n")
        .build()

    val consoleAppender: Appender?
    if (console) {
        consoleAppender = ConsoleAppender.newBuilder()
            .setName("Console")
            .setImmediateFlush(true)
            .setLayout(layout)
            .setFilter(LevelRangeFilter.createFilter(Level.FATAL, levelConsole, null, null))
            .build()
        consoleAppender.start()
        config.addAppender(consoleAppender)
    } else {
        consoleAppender = null
    }

    val rollAppender: RollingFileAppender?
    if (dir == null) {
        rollAppender = null
    } else {
        val policy1 = TimeBasedTriggeringPolicy.newBuilder()
            .withInterval(1)
            .build()
        val policy2 = SizeBasedTriggeringPolicy.createPolicy(singleFileSize)
        val strategy: DefaultRolloverStrategy = DefaultRolloverStrategy.newBuilder()
            .withMax("$maxFileCount")
            .build()
        rollAppender = RollingFileAppender.newBuilder()
            .setName("Roll")
            .withFileName(dir.canonicalPath + "/$baseName.log")
            .withFilePattern(dir.canonicalPath + "/$baseName-%d{yyyy-MM-dd}-%i.log")
            .setLayout(layout)
            .withPolicy(policy1)
            .withPolicy(policy2)
            .withStrategy(strategy)
            .setFilter(LevelRangeFilter.createFilter(Level.FATAL, levelFile, null, null))
            .build()
        rollAppender.start()
        config.addAppender(rollAppender)
    }

    val appenders: Array<AppenderRef> = if (consoleAppender != null && rollAppender != null) {
        arrayOf(AppenderRef.createAppenderRef(consoleAppender.name, null, null), AppenderRef.createAppenderRef(rollAppender.name, null, null))
    } else if (consoleAppender != null) {
        arrayOf(AppenderRef.createAppenderRef(consoleAppender.name, null, null))
    } else if (rollAppender != null) {
        arrayOf(AppenderRef.createAppenderRef(rollAppender.name, null, null))
    } else {
        return
    }

    val entaoLogConfig = LoggerConfig.newBuilder()
        .withLoggerName(minePackage)
        .withLevel(levelMine)
        .withAdditivity(false)
        .withRefs(appenders)
        .withConfig(config)
        .build()

    val rootLogConfig = LoggerConfig.newBuilder()
        .withLoggerName(LoggerConfig.ROOT)
        .withLevel(levelRoot)
        .withAdditivity(false)
        .withRefs(appenders)
        .withConfig(config)
        .build()

    if (consoleAppender != null) {
        entaoLogConfig.addAppender(consoleAppender, null, null)
        rootLogConfig.addAppender(consoleAppender, null, null)
    }
    if (rollAppender != null) {
        entaoLogConfig.addAppender(rollAppender, null, null)
        rootLogConfig.addAppender(rollAppender, null, null)
    }
    config.addLogger(entaoLogConfig.name, entaoLogConfig)
    config.addLogger(rootLogConfig.name, rootLogConfig)
    ctx.updateLoggers()
}


