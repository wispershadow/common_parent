package io.wispershadow.tech.common.http

import io.mockk.mockk
import org.apache.http.protocol.HttpContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.ConnectException
import java.net.SocketException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

class DefaultHttpRequestRetryHandlerTest {
    private val httpContext = mockk<HttpContext>(relaxed = true)

    @Test
    fun testRetriableWithException() {
        val connectionConfig = HttpClientConfig().apply {
            this.retriable = true
            this.maxRetryTimes = 3
            this.retriableExceptions = listOf(SocketException::class.java.name)
        }
        val isRetriable = DefaultHttpRequestRetryHandler(connectionConfig).retryRequest(ConnectException("error"), 2, httpContext)
        Assertions.assertTrue(isRetriable)
    }

    @Test
    fun testRetriableEmptyException() {
        val connectionConfig = HttpClientConfig().apply {
            this.retriable = true
            this.maxRetryTimes = 3
            this.retriableExceptions = emptyList()
        }
        val isRetriable = DefaultHttpRequestRetryHandler(connectionConfig).retryRequest(ConnectException("error"), 0, httpContext)
        Assertions.assertTrue(isRetriable)
    }

    @Test
    fun testNotRetriableFeatureToggle() {
        val connectionConfig = HttpClientConfig().apply {
            this.retriable = false
            this.maxRetryTimes = 3
            this.retriableExceptions = emptyList()
        }
        val isRetriable = DefaultHttpRequestRetryHandler(connectionConfig).retryRequest(SSLException("error"), 1, httpContext)
        Assertions.assertFalse(isRetriable)
    }

    @Test
    fun testNotRetriableExceedLimit() {
        val connectionConfig = HttpClientConfig().apply {
            this.retriable = true
            this.maxRetryTimes = 3
            this.retriableExceptions = emptyList()
        }
        val isRetriable = DefaultHttpRequestRetryHandler(connectionConfig).retryRequest(SSLException("error"), 4, httpContext)
        Assertions.assertFalse(isRetriable)
    }

    @Test
    fun testNotRetriableException1() {
        val connectionConfig = HttpClientConfig().apply {
            this.retriable = true
            this.maxRetryTimes = 3
            this.retriableExceptions = listOf(SSLHandshakeException::class.java.name)
        }
        val isRetriable = DefaultHttpRequestRetryHandler(connectionConfig).retryRequest(SSLException("error"), 2, httpContext)
        Assertions.assertFalse(isRetriable)
    }

    @Test
    fun testNotRetriableException2() {
        val connectionConfig = HttpClientConfig().apply {
            this.retriable = true
            this.maxRetryTimes = 3
            this.retriableExceptions = listOf(SocketException::class.java.name)
        }
        val isRetriable = DefaultHttpRequestRetryHandler(connectionConfig).retryRequest(SSLException("error"), 2, httpContext)
        Assertions.assertFalse(isRetriable)
    }
}