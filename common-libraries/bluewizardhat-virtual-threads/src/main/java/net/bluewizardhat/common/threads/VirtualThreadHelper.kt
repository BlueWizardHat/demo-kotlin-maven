package net.bluewizardhat.common.threads

import org.springframework.core.task.TaskDecorator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun interface JobDecorator {
    fun decorate(runnable: Runnable): Runnable
}

class ExecutorServiceScope private constructor(
    private val executorService: ExecutorService,
    private val decorator: JobDecorator,
    private val closeOnCompletion: Boolean
) : AutoCloseable {
    constructor(executorService: ExecutorService?, decorator: JobDecorator)
        : this(executorService ?: Executors.newVirtualThreadPerTaskExecutor(), decorator, executorService == null)

    override fun close() {
        if (closeOnCompletion) {
            executorService.close()
        }
    }

    fun <T> ExecutorServiceScope.async(job: () -> T): CompletableFuture<T> =
        CompletableFuture<T>().apply {
            executorService.execute {
                decorator.decorate {
                    try {
                        complete(job())
                    } catch (e: Throwable) {
                        completeExceptionally(e)
                    }
                }
            }
        }

    fun ExecutorServiceScope.launch(job: () -> Unit): CompletableFuture<Void?> =
        CompletableFuture.runAsync(decorator.decorate(job), executorService)
}

fun <T> execute(
    executorService: ExecutorService? = null,
    decorator: JobDecorator = JobDecorator { runnable -> runnable },
    block: ExecutorServiceScope.() -> T
): T =
    ExecutorServiceScope(executorService, decorator).use { block(it) }

fun TaskDecorator.toJobDecorator() = JobDecorator { runnable -> decorate(runnable) }


fun stuff() {
    execute {
        val f1 = launch { println("") }
        val f2 = async { "" }
    }
}
