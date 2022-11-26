package net.bluewizardhat.common.logging.aspect.resulthandling

import net.bluewizardhat.common.logging.aspect.LogWrapper
import java.util.concurrent.CompletableFuture

class CompletableFutureResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is CompletableFuture<*>

    override fun handleInternal(log: LogWrapper, methodName: String, startTime: Long, result: Any): Any {
        val actualResult = result as CompletableFuture<*>

        val loggingResult = object : CompletableFuture<Any>() {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                val wasCancelled = actualResult.cancel(mayInterruptIfRunning)
                if (wasCancelled) {
                    log.log("Cancelled {}", methodName)
                }
                return wasCancelled
            }
            override fun isCancelled(): Boolean = actualResult.isCancelled
        }

        actualResult.handle { value: Any, throwable: Throwable? ->
            if (throwable != null) {
                log.logErrorReturn(methodName, startTime, throwable)
                loggingResult.completeExceptionally(throwable)
            } else {
                log.logNormalReturn(methodName, startTime, value)
                loggingResult.complete(value)
            }
        }

        log.logger.debug("<- Exiting {} async with CompletableFuture - processing may continue in another thread", methodName)
        return loggingResult
    }
}
