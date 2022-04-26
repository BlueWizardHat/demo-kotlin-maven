package net.bluewizardhat.common.logging.aspect.resulthandling

import mu.KotlinLogging
import net.bluewizardhat.common.logging.aspect.LogWrapper

abstract class ResultHandler {
    abstract fun canHandle(result: Any): Boolean
    protected abstract fun handleInternal(log: LogWrapper, methodName: String, startTime: Long, result: Any): Any

    fun handle(log: LogWrapper, methodName: String, startTime: Long, result: Any): Any {
        try {
            return handleInternal(log, methodName, startTime, result)
        } catch (thr: Throwable) {
            log.logger.warn("Unable to handle ${result.javaClass.name}", thr)
        }
        return result
    }
}

/**
 * Default that handles all unknown types and null.
 */
class DefaultResultHandler {
    fun handle(log: LogWrapper, methodName: String, startTime: Long, result: Any?): Any? {
        log.logNormalReturn(methodName, startTime, result)
        return result
    }
}

/**
 * Handles async return types we recognize that we cannot intercept for a reliable time measurement.
 */
class NonInterceptableAsyncResultHandler : ResultHandler() {
    private val log = KotlinLogging.logger {}
    private val asyncTypes: List<Class<*>> = sequenceOf(
        "java.util.concurrent.Future",
        "java.util.concurrent.Flow",
        "java.util.concurrent.Flow\$Publisher",
        "org.reactivestreams.Publisher"
    ).map {
        try {
            this.javaClass.classLoader.loadClass(it)
        } catch (e: ClassNotFoundException) {
            null
        }
    }.filterNotNull().toList()

    init {
        log.info { "Initialized to handle [${asyncTypes.joinToString { it.name }}]" }
    }

    override fun canHandle(result: Any): Boolean {
        val resultClass = result.javaClass
        return asyncTypes.firstOrNull { it.isAssignableFrom(resultClass) } != null
    }

    override fun handleInternal(log: LogWrapper, methodName: String, startTime: Long, result: Any): Any {
        log.log("<- Exiting {} async with {} - processing may continue in another thread", methodName, result.javaClass.simpleName)
        return result
    }
}
