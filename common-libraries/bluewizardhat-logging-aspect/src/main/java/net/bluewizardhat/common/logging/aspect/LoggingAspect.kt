package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.resulthandling.DefaultResultHandler
import net.bluewizardhat.common.logging.aspect.resulthandling.FutureResultHandler
import net.bluewizardhat.common.logging.aspect.resulthandling.ListenableFutureResultHandler
import net.bluewizardhat.common.logging.aspect.resulthandling.ResultHandler
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.util.LinkedList

@Aspect
@Component
class LoggingAspect {
    companion object {
        @LogInvocation
        class LogInvocationDefaults
    }

    private val resultHandlers: LinkedList<ResultHandler> =
        LinkedList<ResultHandler>().apply {
            add(ListenableFutureResultHandler())
            add(FutureResultHandler())
        }
    private val defaultResultHandler: DefaultResultHandler = DefaultResultHandler()

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

        val log = LogWrapper(logger, annotation)

        try {
            when {
                args.isEmpty() -> log.log("-> Entering {}()", method.name)
                annotation.logParameterValues -> log.log("-> Entering {}({})", method.name) { getLoggableArgs(methodSignature, method, args) }
                else -> log.log("-> Entering {}({})", method.name) { getParamNamesOrTypes(methodSignature, method) }
            }

            val result = jp.proceed()
            val millis = System.currentTimeMillis() - startTime

            return resultHandlers.find { it.canHandle(result) }?.handle(log, method.name, millis, result)
                ?: defaultResultHandler.handle(log, method.name, millis, result)
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
}
