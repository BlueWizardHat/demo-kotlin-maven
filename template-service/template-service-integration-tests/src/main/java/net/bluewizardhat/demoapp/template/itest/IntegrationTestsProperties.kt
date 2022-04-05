package net.bluewizardhat.demoapp.template.itest

import java.util.Properties

object IntegrationTestsProperties {
    val endpoint: String by lazy {
        val props = Properties()
        ClassLoader.getSystemResourceAsStream("integration-tests.properties").use {
            props.load(it)
        }
        props.getProperty("integration.tests.endpoint")
    }
}
