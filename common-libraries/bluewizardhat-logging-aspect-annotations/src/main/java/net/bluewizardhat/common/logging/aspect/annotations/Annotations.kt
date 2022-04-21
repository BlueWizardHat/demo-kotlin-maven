package net.bluewizardhat.common.logging.aspect.annotations

/**
 * Indicates that a parameter is sensitive and therefore it's value should not be logged.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Sensitive

/**
 * Log only the type of the parameter value, but not the actual value.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class HideValue

/**
 * Annotation to make the logging aspect log entry and exit of methods. When put on a class applies to all public methods
 * of that class. When put both a class and a method in the same class the values for the method takes priority.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class LogCall(
    /**
     * Normal messages (Entering, Exiting, etc.) are logged at this level. Default is INFO.
     */
    val logLevel: LogLevel = LogLevel.INFO,
    /**
     * Log parameter values (parameters annotated with @Sensitive will have their values masked).
     *
     * Default `true` because it's very useful.
     */
    val logParameterValues: Boolean = true,
    /**
     * Exceptions are logged at this level. Default is WARN.
     */
    val exceptionLogLevel: ExceptionLogLevel = ExceptionLogLevel.WARN,
    /**
     * Log stacktraces.
     *
     * Exceptions are re-thrown by the logging aspect so full stacktraces should be caught and logged at
     * a higher level.
     *
     * Default `false` to avoid excessive logging.
     */
    val logStacktraces: Boolean = false
) {
    enum class LogLevel {
        TRACE, DEBUG, INFO
    }

    enum class ExceptionLogLevel {
        WARN, ERROR
    }
}
