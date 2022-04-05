package net.bluewizardhat.demoapp.template.itest

import org.junit.platform.engine.discovery.ClassNameFilter
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import org.junit.platform.launcher.listeners.discovery.LauncherDiscoveryListeners
import java.io.PrintWriter
import kotlin.system.exitProcess

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("\nRunning Tests for Template-Service - ${IntegrationTestsProperties.endpoint}")

            val summary = Main().runTests()
            if (summary.testsFailedCount > 0) {
                summary.printFailuresTo(PrintWriter(System.out))
                exitProcess(1)
            }
            exitProcess(0)
        }
    }

    private fun runTests(): TestExecutionSummary {
        val listener = SummaryGeneratingListener()

        val launcher = LauncherFactory.create()
        launcher.registerLauncherDiscoveryListeners(LauncherDiscoveryListeners.logging(), LauncherDiscoveryListeners.abortOnFailure())
        val testplan = launcher.discover(
            LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage(this.javaClass.packageName))
                .filters(ClassNameFilter.includeClassNamePatterns(".*Test"))
                .build()
        )
        launcher.registerTestExecutionListeners(listener)
        launcher.execute(testplan)

        return listener.summary.apply {
            printTo(PrintWriter(System.out))
        }
    }
}
