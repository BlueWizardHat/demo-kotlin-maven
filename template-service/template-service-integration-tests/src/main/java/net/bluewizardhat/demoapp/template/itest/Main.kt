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

/**
 * Takes one argument - the baseUrl to run tests against.
 */
class Main {
    companion object {
        lateinit var baseUrl: String

        @JvmStatic
        fun main(args: Array<String>) {

            if (args.size != 1) {
                println("No target")
                exitProcess(1)
            }
            baseUrl = args[0]
            println("Running Tests for Template-Service - $baseUrl")

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
