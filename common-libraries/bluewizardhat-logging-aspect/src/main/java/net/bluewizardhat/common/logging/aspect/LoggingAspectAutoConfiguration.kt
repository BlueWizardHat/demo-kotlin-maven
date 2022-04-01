package net.bluewizardhat.common.logging.aspect

import net.bluewizardhat.common.logging.aspect.resulthandling.DeferredResultResultHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
class LoggingAspectAutoConfiguration {
    @Bean
    @ConditionalOnClass(name = ["org.springframework.web.context.request.async.DeferredResult"])
    fun loadDeferredResultResultHandler(aspect: LoggingAspect): DeferredResultResultHandler? {
        val resultHandler = DeferredResultResultHandler()
        aspect.registerResultHandler(resultHandler)
        return null // Hacky
    }
}
