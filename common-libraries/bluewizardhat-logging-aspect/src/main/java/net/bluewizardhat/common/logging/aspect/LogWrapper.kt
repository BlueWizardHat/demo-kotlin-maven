package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.LogInvocation.ExceptionLogLevel.ERROR
import net.bluewizardhat.common.logging.aspect.LogInvocation.ExceptionLogLevel.WARN
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

    fun logErrorReturn(methodName: String, millis: Long, thr: Throwable) {
        val (thrMsg, chain) = describeThrowable(thr)

        val args: Array<*> =
            if (annotation.logStacktraces)
                arrayOf(methodName, millis, chain, thrMsg, thr)
            else
                arrayOf(methodName, millis, chain, thrMsg)

        when (annotation.exceptionLogLevel) {
            ERROR -> logger.error("<- Exiting {} (after {} ms) with {}: '{}'", *args)
            WARN -> logger.warn("<- Exiting {} (after {} ms) with {}: '{}'", *args)
        }
    }

    private fun describeThrowable(original: Throwable): Pair<String?, String> {
        val chain = StringBuilder(original.javaClass.simpleName)
        var lastCause: Throwable = original
        var depth = 1

        while (lastCause.cause != null) {
            depth++
            lastCause = lastCause.cause!!
        }

        if (depth == 2) {
            chain.append(" < ").append(lastCause.javaClass.simpleName)
        } else if (depth > 2) {
            chain.append(" <-< ").append(lastCause.javaClass.simpleName)
        }

        return Pair(lastCause.message ?: "<null>", chain.toString())
    }
}
