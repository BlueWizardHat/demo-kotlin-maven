package net.bluewizardhat.demoapp.template.itest

/**
 * Takes one argument - the baseUrl to run tests against.
 */
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size != 1) {
                println("No target")
                System.exit(1)
            }
            println("Running Tests for Template-Service - ${args[0]}")
            System.exit(0)
            // if (result.wasSuccessful()) {
            //   System.exit(0)
            // } else {
            //     println(result.failures)
            //     System.exit(1)
            // }
        }
    }
}
