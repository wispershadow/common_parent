package io.wispershadow.tech.common.stream

import java.util.stream.Stream

// used to ensure that a stream is closed after iteration while converting
// Eg. jdbcTemplate.queryForStream, by default, without calling the stream's close
// method, it will cause connection leak. But sometimes, this is difficult if we
// convert the stream to kotlin sequence and return the sequence from method call
// usage:  val stream = jdbcTemplate.queryForStream(xxx)
// stream.asClosableSequence().forEach {}
object ClosableStreamUtils {
    public fun <T> Stream<T>.asClosableSequence(): Sequence<T> = Sequence {
        AutoCloseIterator(this, iterator())
    }
}

class AutoCloseIterator<T>(val closable: AutoCloseable,
                           val nestedIterator: Iterator<T>): Iterator<T> {
    override fun hasNext(): Boolean {
        val hasNext= nestedIterator.hasNext()
        if (!hasNext) {
            doClose()
        }
        return hasNext
    }

    override fun next(): T {
        try {
            return nestedIterator.next()
        }
        catch (e: Exception) {
            doClose()
            throw e
        }
    }

    private fun doClose() {
        try {
            closable.close()
        }
        catch (e: Exception) {
        }
    }
}