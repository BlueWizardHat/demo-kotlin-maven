package net.bluewizardhat.demoapp.template.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfiguration {
    private val log = KotlinLogging.logger {}

    @Bean
    fun defaultObjectMapper(): ObjectMapper {
        log.info { "Creating default ObjectMapper" }
        return ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(ParameterNamesModule())
            .registerModule(KotlinModule.Builder().build())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // ISO timestamps please
    }
}
