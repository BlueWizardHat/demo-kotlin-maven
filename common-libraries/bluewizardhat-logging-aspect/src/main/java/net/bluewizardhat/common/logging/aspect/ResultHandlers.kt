package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.DEBUG
import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.INFO
import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.TRACE
import org.slf4j.Logger

abstract class ResultHandler {
    abstract fun canHandle(result: Any): Boolean
    abstract fun handle(logger: Logger, annotation: LogInvocation, methodName: String, millis: Long, result: Any)

    protected fun log(logger: Logger, annotation: LogInvocation, msg: String, vararg args: Any) {
        when (annotation.logLevel) {
            TRACE -> if (logger.isTraceEnabled) { logger.trace(msg, *args) }
            DEBUG -> if (logger.isDebugEnabled) { logger.debug(msg, *args) }
            INFO -> if (logger.isInfoEnabled) { logger.info(msg, *args) }
        }
    }
}

class DefaultResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = true

    override fun handle(logger: Logger, annotation: LogInvocation, methodName: String, millis: Long, result: Any) =
        log(logger, annotation, "<- Exiting {} (after {} ms) without errors", methodName, millis)
}
