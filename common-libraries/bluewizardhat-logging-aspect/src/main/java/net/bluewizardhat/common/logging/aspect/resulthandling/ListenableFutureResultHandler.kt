package net.bluewizardhat.common.logging.aspect.resulthandling

import net.bluewizardhat.common.logging.aspect.LogWrapper
import org.springframework.util.concurrent.ListenableFuture

class ListenableFutureResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is ListenableFuture<*>

    override fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any) {
        log.log("<- Exiting {} async with Spring ListenableFuture - processing may continue in another thread", methodName)
    }
}
