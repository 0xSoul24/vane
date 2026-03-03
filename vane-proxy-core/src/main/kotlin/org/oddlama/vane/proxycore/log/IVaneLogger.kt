package org.oddlama.vane.proxycore.log

import java.util.logging.Level

interface IVaneLogger {
    fun log(level: Level?, message: String?)

    fun log(level: Level?, message: String?, throwable: Throwable?)
}
