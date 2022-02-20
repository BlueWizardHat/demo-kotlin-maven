package net.bluewizardhat.demoapp.template

import net.bluewizardhat.common.cache.RedisCacheConfiguration
import net.bluewizardhat.common.logging.aspect.LoggingAspectConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(RedisCacheConfiguration::class, LoggingAspectConfiguration::class)
class Application {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<Application>(*args)
        }
    }
}
