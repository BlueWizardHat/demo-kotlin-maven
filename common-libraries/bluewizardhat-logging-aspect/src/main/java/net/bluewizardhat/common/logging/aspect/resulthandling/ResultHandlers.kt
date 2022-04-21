package net.bluewizardhat.common.logging.aspect.resulthandling

import mu.KotlinLogging
import net.bluewizardhat.common.logging.aspect.LogWrapper

abstract class ResultHandler {
    abstract fun canHandle(result: Any): Boolean
    protected abstract fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any)

    fun handle(log: LogWrapper, methodName: String, millis: Long, result: Any): Any {
        try {
            handleInternal(log, methodName, millis, result)
        } catch (thr: Throwable) {
            // log.error("", thr)
        }
        return result
    }
}

/**
 * Default that handles all unknown types and null.
 */
class DefaultResultHandler {
    fun handle(log: LogWrapper, methodName: String, millis: Long, result: Any?): Any? {
        log.log("<- Exiting $methodName after $millis ms with ${result?.javaClass?.simpleName ?: "<null>"}")
        return result
    }
}

/**
 * Handles async return types we recognize that we cannot patch for a reliable time measurement.
 */
class AsyncResultHandler : ResultHandler() {
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

    override fun handleInternal(log: LogWrapper, methodName: String, millis: Long, result: Any) {
        log.log("<- Exiting $methodName async with ${result.javaClass.simpleName} after $millis ms - processing may continue in another thread")
    }
}
