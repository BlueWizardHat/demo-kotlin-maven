package net.bluewizardhat.demoapp.template.client

import feign.Client
import feign.okhttp.OkHttpClient
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan
class TemplateServiceClientConfiguration {
    private val log = KotlinLogging.logger {}

    @Bean(name = ["feignOkHttpClient"])
    @ConditionalOnMissingBean(name = ["feignOkHttpClient"])
    fun feignOkHttpClient(): Client {
        log.info { "Initialized Feign OkHttpClient" }
        return OkHttpClient()
    }
}
