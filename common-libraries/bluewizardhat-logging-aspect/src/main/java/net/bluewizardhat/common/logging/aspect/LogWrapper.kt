package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.annotations.LogCall
import net.bluewizardhat.common.logging.aspect.annotations.LogCall.ExceptionLogLevel.ERROR
import net.bluewizardhat.common.logging.aspect.annotations.LogCall.ExceptionLogLevel.WARN
import net.bluewizardhat.common.logging.aspect.annotations.LogCall.LogLevel.DEBUG
import net.bluewizardhat.common.logging.aspect.annotations.LogCall.LogLevel.INFO
import net.bluewizardhat.common.logging.aspect.annotations.LogCall.LogLevel.TRACE
import org.slf4j.Logger
import java.util.function.Supplier

class LogWrapper(
    val logger: Logger,
    private val annotation: LogCall
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

    fun logNormalReturn(methodName: String, startTime: Long, value: Any?) {
        val millis = System.currentTimeMillis() - startTime
        log("<- Exiting $methodName after $millis ms with ${value?.javaClass?.simpleName ?: "<null>"}")
    }

    fun logErrorReturn(methodName: String, startTime: Long, thr: Throwable) {
        val millis = System.currentTimeMillis() - startTime
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
            chain.append(" << ").append(lastCause.javaClass.simpleName)
        }

        return Pair(lastCause.message ?: "<null>", chain.toString())
    }
}
