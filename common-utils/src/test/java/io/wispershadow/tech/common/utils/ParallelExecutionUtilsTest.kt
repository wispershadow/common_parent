package io.wispershadow.tech.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

object ParallelExecutionUtilsTest {
    private val logger: Logger = LoggerFactory.getLogger(ParallelExecutionUtilsTest::class.java)

    @Test
    fun testIgnoreStrategy() {
        val parameters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val errorCount = AtomicInteger()
        val result = ParallelExecutionUtils.executeAllInParallel({input: Int ->
            loadWithRandomError(input, errorCount)
        }, parameters)
        logger.info("Execution result is: {}", result)
        logger.info("Number of errors: {}", errorCount)
        Assertions.assertEquals(result.size, parameters.size - errorCount.get())
    }

    @Test
    fun testStopStrategy() {
        val parameters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val errorCount = AtomicInteger()
        try {
            val result = ParallelExecutionUtils.executeAllInParallel({ input: Int ->
                loadWithRandomError(input, errorCount)
            }, parameters, ParallelErrorPropagationStrategy.EXEC_ALL_PROPAGATE)
            if (errorCount.get() > 0) {
                Assertions.fail<String>("Expected exception thrown from parallel exception")
            }
        }
        catch(e: Exception) {
            if (e is ParallelExecutionException) {
                Assertions.assertEquals(errorCount.get(), e.nestedExceptions.size)
            }
            else {
                Assertions.fail<String>("Excepted exception type: ParallelExecutionException")
            }
        }
    }

    fun loadWithRandomError(input: Int, errorCount: AtomicInteger): String {
        val randomValue = Math.random()
        if (randomValue < 0.5) {
            return input.toString()
        }
        else {
            println("error -----" + input)
            errorCount.incrementAndGet()
            throw RuntimeException("")
        }
    }
}