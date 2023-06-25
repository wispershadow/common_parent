package io.wispershadow.tech.common.utils

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

object ParallelExecutionUtils {
    fun <I, O> executeAllInParallel(singleExecFunction: (I) -> O,
                                    params: Collection<I>, errorPropagationStrategy: ParallelErrorPropagationStrategy = ParallelErrorPropagationStrategy.IGNORE): Map<I, O> {
        val resultMap = ConcurrentSkipListMap<I, O>()
        if (errorPropagationStrategy == ParallelErrorPropagationStrategy.IGNORE) {
            val supervisor = SupervisorJob()
            runBlocking {
                val actualContext: CoroutineContext = coroutineContext + supervisor
                val asyncScope = CoroutineScope(actualContext)
                val job = launch {
                    params.forEach { param ->
                        asyncScope.async {
                            resultMap[param] = singleExecFunction.invoke(param)
                        }
                    }
                }
                job.join()
            }
        }
        else if (errorPropagationStrategy == ParallelErrorPropagationStrategy.STOP_ON_FIRST) {
            runBlocking {
                val job = launch {
                    params.forEach { param ->
                        async {
                            resultMap[param] = singleExecFunction.invoke(param)
                        }
                    }
                }
                job.join()
            }
        }
        else if (errorPropagationStrategy == ParallelErrorPropagationStrategy.EXEC_ALL_PROPAGATE) {
            val exceptions = ConcurrentSkipListMap<I, Exception>()
            runBlocking {
                val job = launch {
                    params.forEach { param ->
                        async {
                            try {
                                resultMap[param] = singleExecFunction.invoke(param)
                            }
                            catch (e: Exception) {
                                exceptions[param] = e
                                throw CancellationException()
                            }
                        }
                    }
                }
                job.join()
            }
            if (exceptions.isNotEmpty()) {
                throw ParallelExecutionException(exceptions.values.toSet())
            }
        }
        return resultMap
    }
}


class ParallelExecutionException(val nestedExceptions: Set<Exception>): RuntimeException() {

}

enum class ParallelErrorPropagationStrategy {
    IGNORE,
    STOP_ON_FIRST,
    EXEC_ALL_PROPAGATE
}