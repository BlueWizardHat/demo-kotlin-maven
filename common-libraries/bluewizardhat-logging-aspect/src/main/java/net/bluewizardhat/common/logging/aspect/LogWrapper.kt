package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.DEBUG
import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.INFO
import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.TRACE
import org.slf4j.Logger
import java.util.function.Supplier

class LogWrapper(
    private val logger: Logger,
    private val annotation: LogInvocation
) {

    fun log(msg: String, first: String, args: Supplier<String>) {
        when (annotation.logLevel) {
            TRACE -> if (logger.isTraceEnabled) { logger.trace(msg, first, args.get()) }
            DEBUG -> if (logger.isDebugEnabled) { logger.debug(msg, first, args.get()) }
            INFO -> if (logger.isInfoEnabled) { logger.info(msg, first, args.get()) }
        }
    }

    fun log(msg: String, vararg args: Any) {
        when (annotation.logLevel) {
            TRACE -> if (logger.isTraceEnabled) { logger.trace(msg, *args) }
            DEBUG -> if (logger.isDebugEnabled) { logger.debug(msg, *args) }
            INFO -> if (logger.isInfoEnabled) { logger.info(msg, *args) }
        }
    }
}
