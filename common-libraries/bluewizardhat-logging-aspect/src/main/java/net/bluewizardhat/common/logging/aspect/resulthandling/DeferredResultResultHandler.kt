package net.bluewizardhat.common.logging.aspect.resulthandling

import net.bluewizardhat.common.logging.aspect.LogWrapper
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.context.request.async.DeferredResult
import java.lang.reflect.Field
import java.util.function.Consumer
import java.util.function.Supplier

class DeferredResultResultHandler : ResultHandler() {
    override fun canHandle(result: Any): Boolean = result is DeferredResult<*>

    override fun handleInternal(log: LogWrapper, methodName: String, startTime: Long, result: Any): Any {
        val actualResult = result as DeferredResult<*>
        val timeoutValue = getField<Long>(log, actualResult, "timeoutValue")
        val timeoutResult = getField<Supplier<*>>(log, actualResult, "timeoutResult")

        val loggingResult = DeferredResult<Any>(timeoutValue, timeoutResult)

        actualResult.setResultHandler {
            when (it) {
                is Throwable -> log.logErrorReturn(methodName, startTime, it)
                else -> log.logNormalReturn(methodName, startTime, it)
            }
            loggingResult.setResult(it)
        }

        loggingResult.onTimeout {
            try {
                getField<Runnable>(log, actualResult, "timeoutCallback")?.run()
            } finally {
                if (!result.hasResult()) {
                    // Would have been set by Spring if we had not intercepted.
                    result.setErrorResult(AsyncRequestTimeoutException())
                }
            }
        }

        loggingResult.onError {
            getField<Consumer<Throwable>>(log, actualResult, "errorCallback")?.accept(it)
        }

        loggingResult.onCompletion {
            getField<Runnable>(log, actualResult, "completionCallback")?.run()
        }

        log.logger.debug("<- Exiting {} async with Spring DeferredResult - processing may continue in another thread", methodName)
        return loggingResult
    }

    private fun <T : Any> getField(log: LogWrapper, result: DeferredResult<*>, name: String): T? {
        return try {
            val field: Field = DeferredResult::class.java.getDeclaredField(name)
            field.trySetAccessible()
            field.get(result) as T?
        } catch (e: Exception) {
            log.logger.warn("Unable to read DeferredResult.$name - ${e.javaClass.name}: ${e.message}")
            null
        }
    }
}
