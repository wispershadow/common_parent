package io.wispershadow.tech.common.stream

import io.mockk.every
import io.mockk.mockk
import io.wispershadow.tech.common.stream.ClosableStreamUtils.asClosableSequence
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.stream.Stream

class ClosableStreamUtilsTest {
    @Test
    fun testStream() {
        var closed = false
        val stream = mockk<Stream<String>>(relaxed = true)
        every { stream.close() } answers {
            closed = true
        }
        stream.asClosableSequence().forEach {
            println(it)
        }
        Assertions.assertEquals(closed, true)
    }
}