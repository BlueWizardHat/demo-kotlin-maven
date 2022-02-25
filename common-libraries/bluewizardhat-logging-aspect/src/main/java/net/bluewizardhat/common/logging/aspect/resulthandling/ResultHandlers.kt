package net.bluewizardhat.common.logging.aspect.resulthandling

import net.bluewizardhat.common.logging.aspect.LogWrapper
import org.springframework.util.concurrent.ListenableFuture
import java.util.concurrent.Future

abstract class ResultHandler {
    abstract fun canHandle(result: Any): Boolean
    protected abstract fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any): Any

    fun handle(log: LogWrapper, methodName: String, millis: Long, result: Any): Any =
        try {
            handleInternal(log, methodName, millis, result)
        } catch (thr: Throwable) {
            // log.error("", thr)
            result
        }
}

class DefaultResultHandler {
    fun handle(log: LogWrapper, methodName: String, millis: Long, result: Any?): Any? {
        log.log("<- Exiting {} after {} ms without errors", methodName, millis)
        return result
    }
}

class FutureResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is Future<*>

    override fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any): Any {
        log.log("<- Exiting {} async with Future - processing may continue in another thread", methodName)
        return result
    }
}

class ListenableFutureResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is ListenableFuture<*>

    override fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any): Any {
        log.log("<- Exiting {} async with Spring ListenableFuture - processing may continue in another thread", methodName)
        return result
    }
}
