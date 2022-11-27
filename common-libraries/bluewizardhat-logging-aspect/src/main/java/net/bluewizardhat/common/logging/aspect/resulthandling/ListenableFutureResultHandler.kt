package net.bluewizardhat.common.logging.aspect.resulthandling

import net.bluewizardhat.common.logging.aspect.LogWrapper
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.SettableListenableFuture

class ListenableFutureResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is ListenableFuture<*>

    override fun handleInternal(log: LogWrapper, methodName: String, startTime: Long, result: Any): Any {
        val actualResult = result as ListenableFuture<*>

        val loggingResult = object : SettableListenableFuture<Any>() {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                val wasCancelled = actualResult.cancel(mayInterruptIfRunning)
                if (wasCancelled) {
                    log.log("Cancelled {}", methodName)
                }
                return wasCancelled
            }
            override fun isCancelled(): Boolean = actualResult.isCancelled
        }

        actualResult.addCallback(
            { value ->
                log.logNormalReturn(methodName, startTime, value)
                loggingResult.set(value)
            },
            { t ->
                log.logErrorReturn(methodName, startTime, t)
                loggingResult.setException(t)
            }
        )

        log.logger.trace("<- Exiting {} async with Spring ListenableFuture - processing may continue in another thread", methodName)
        return loggingResult
    }
}
