package org.oddlama.vane.proxycore.log

import java.util.logging.Level
import java.util.logging.Logger

class JavaCompatLogger(private val logger: Logger) : IVaneLogger {
    override fun log(level: Level?, message: String?) {
        logger.log(level, message)
    }

    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        logger.log(level, message, throwable)
    }
}
