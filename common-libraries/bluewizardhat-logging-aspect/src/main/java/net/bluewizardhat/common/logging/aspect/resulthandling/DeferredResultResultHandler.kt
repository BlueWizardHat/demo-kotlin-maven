package net.bluewizardhat.common.logging.aspect.resulthandling

import net.bluewizardhat.common.logging.aspect.LogWrapper
import org.springframework.web.context.request.async.DeferredResult

class DeferredResultResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is DeferredResult<*>

    override fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any): Any {
        log.log("<- Exiting {} async with Spring DeferredResult - processing may continue in another thread", methodName)
        return result
    }
}
