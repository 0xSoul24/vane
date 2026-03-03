package org.oddlama.vane.proxycore.log

import org.slf4j.Logger
import java.util.logging.Level

class Slf4jCompatLogger(private val logger: Logger) : IVaneLogger {
    override fun log(level: Level?, message: String?) {
        if (Level.INFO == level) {
            logger.info(message)
        } else if (Level.WARNING == level) {
            logger.warn(message)
        } else if (Level.SEVERE == level) {
            logger.error(message)
        }
    }

    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        if (Level.INFO == level) {
            logger.info(message, throwable)
        } else if (Level.WARNING == level) {
            logger.warn(message, throwable)
        } else if (Level.SEVERE == level) {
            logger.error(message, throwable)
        }
    }
}
