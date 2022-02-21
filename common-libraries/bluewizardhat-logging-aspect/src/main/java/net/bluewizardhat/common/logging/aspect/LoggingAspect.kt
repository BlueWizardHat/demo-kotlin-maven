package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.DEBUG
import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.INFO
import net.bluewizardhat.common.logging.aspect.LogInvocation.LogLevel.TRACE
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.LinkedList
import java.util.function.Supplier

@Aspect
@Component
class LoggingAspect {
    companion object {
        @LogInvocation
        class LogInvocationDefaults
    }

    private val resultHandlers: LinkedList<ResultHandler> =
        LinkedList<ResultHandler>().apply {
            add(DefaultResultHandler())
        }

    @Pointcut("execution(public * (@org.springframework.web.bind.annotation.RestController *).*(..))")
    fun methodOfRestController() {}

    @Pointcut("execution(public * (@net.bluewizardhat.common.logging.aspect.LogInvocation *).*(..))")
    fun methodOfAnnotatedClass() {}

    @Pointcut("execution(@net.bluewizardhat.common.logging.aspect.LogInvocation * *.*(..))")
    fun annotatedMethod() {}

    fun registerResultHandler(resultHandler: ResultHandler) = resultHandlers.addFirst(resultHandler)

    @Around("methodOfAnnotatedClass() || annotatedMethod() || methodOfRestController()")
    fun logInvocation(jp: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val args = jp.args
        val methodSignature = jp.signature as MethodSignature
        val method = methodSignature.method
        val logger = LoggerFactory.getLogger(method.declaringClass)

        val annotation: LogInvocation = method.getAnnotation(LogInvocation::class.java)
            ?: method.declaringClass.getAnnotation(LogInvocation::class.java)
            ?: LogInvocationDefaults::class.java.getAnnotation(LogInvocation::class.java)

        try {
            when {
                args.isEmpty() -> log(logger, annotation, "-> Entering {}()", method.name)
                annotation.logParameterValues -> log(logger, annotation, "-> Entering {}({})", method.name) { getLoggableArgs(methodSignature, method, args) }
                else -> log(logger, annotation, "-> Entering {}({})", method.name) { getParamNamesOrTypes(methodSignature, method) }
            }

            val result = jp.proceed()
            val millis = System.currentTimeMillis() - startTime
            resultHandlers.find { it.canHandle(result) }?.handle(logger, annotation, method.name, millis, result)

            return result
        } catch (thr: Throwable) {
            throw thr
        }
    }

    private fun getParamNamesOrTypes(methodSignature: MethodSignature, method: Method): String =
        methodSignature.parameterNames?.joinToString() ?: method.parameterTypes.joinToString { it.simpleName }

    private fun getLoggableArgs(methodSignature: MethodSignature, method: Method, args: Array<Any?>): String {
        val result = StringBuilder()
        val parameters: Array<Parameter> = method.parameters
        val paramNames: Array<String>? = methodSignature.parameterNames

        for (i in args.indices) {
            if (result.isNotEmpty()) result.append(",")
            val sensitive = parameters[i].getAnnotation(Sensitive::class.java) != null
            if (paramNames != null && paramNames.size > i) {
                result.append(paramNames[i]).append("=")
            }
            args[i].let {
                when {
                    it == null -> result.append("<null>")
                    sensitive -> result.append("<hidden>")
                    else -> result.append("'").append(it.toString()).append("'")
                }
            }
        }
        return result.toString()
    }

    private fun log(logger: Logger, annotation: LogInvocation, msg: String, first: String, args: Supplier<String>) {
        when (annotation.logLevel) {
            TRACE -> if (logger.isTraceEnabled) { logger.trace(msg, first, args.get()) }
            DEBUG -> if (logger.isDebugEnabled) { logger.debug(msg, first, args.get()) }
            INFO -> if (logger.isInfoEnabled) { logger.info(msg, first, args.get()) }
        }
    }

    private fun log(logger: Logger, annotation: LogInvocation, msg: String, vararg args: Any) {
        when (annotation.logLevel) {
            TRACE -> if (logger.isTraceEnabled) { logger.trace(msg, *args) }
            DEBUG -> if (logger.isDebugEnabled) { logger.debug(msg, *args) }
            INFO -> if (logger.isInfoEnabled) { logger.info(msg, *args) }
        }
    }
}
