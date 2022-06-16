package io.wispershadow.tech.common.rest

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.httpcomponents.MicrometerHttpRequestExecutor
import io.micrometer.core.instrument.binder.httpcomponents.PoolingHttpClientConnectionManagerMetricsBinder
import io.wispershadow.tech.common.http.DefaultHttpRequestRetryHandler
import io.wispershadow.tech.common.http.HttpClientConfig
import org.apache.http.client.HttpClient
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.config.Registry
import org.apache.http.config.RegistryBuilder
import org.apache.http.conn.socket.ConnectionSocketFactory
import org.apache.http.conn.socket.PlainConnectionSocketFactory
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContextBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.lang.Exception
import java.time.Duration
import java.util.*

object RestTemplateCreateHelper {
    private val logger: Logger = LoggerFactory.getLogger(RestTemplateCreateHelper::class.java)

    fun createRestTemplate(restTemplateBuilder: RestTemplateBuilder,
                           connectionConfig: HttpClientConfig,
                           objectMapper: ObjectMapper,
                           name: String,
                           meterRegistryOptional: Optional<MeterRegistry>
    ): RestTemplate {
        return restTemplateBuilder.setConnectTimeout(
            Duration.ofMillis(connectionConfig.connectionTimeoutMillis)
        )
            .setReadTimeout(Duration.ofMillis(connectionConfig.readTimeoutMillis))
            .requestFactory { buildClientHttpRequestFactory(connectionConfig, name, meterRegistryOptional) }
            .customizers(RestTemplateCustomizer { restTemplate: RestTemplate ->
                val currentConverters = restTemplate.messageConverters
                for (messageConverter in currentConverters) {
                    if (messageConverter is AbstractJackson2HttpMessageConverter) {
                        messageConverter.objectMapper = objectMapper
                    }
                }
            })
            .build()
    }

    private fun buildRetryHandler(connectionConfig: HttpClientConfig): HttpRequestRetryHandler {
        return DefaultHttpRequestRetryHandler(connectionConfig)
    }

    private fun buildClientHttpRequestFactory(
        connectionConfig: HttpClientConfig,
        name: String,
        meterRegistryOptional: Optional<MeterRegistry>,
    ): ClientHttpRequestFactory? {
        val acceptingTrustStrategy = TrustStrategy { x509Certificates, s -> true }
        val builder = SSLContextBuilder()
        try {
            builder.loadTrustMaterial(null, acceptingTrustStrategy)
        } catch (e: Exception) {
            logger.error("Pooling Connection Manager Initialisation failure because of " + e.message, e)
        }
        var sslsf: SSLConnectionSocketFactory? = null
        try {
            sslsf = SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier())
        } catch (e: Exception) {
            logger.error("Pooling Connection Manager Initialisation failure because of " + e.message, e)
        }
        val socketFactoryRegistry: Registry<ConnectionSocketFactory?> = RegistryBuilder
            .create<ConnectionSocketFactory?>().register("https", sslsf)
            .register("http", PlainConnectionSocketFactory())
            .build()
        val httpClientConnectionManager = PoolingHttpClientConnectionManager(socketFactoryRegistry).apply {
            this.defaultMaxPerRoute = connectionConfig.maxConnectionPerRoute
            this.maxTotal = connectionConfig.maxTotal
        }

        val httpClientBuilder = HttpClients.custom().setConnectionManager(httpClientConnectionManager)
            .setRetryHandler(buildRetryHandler(connectionConfig))
        meterRegistryOptional.map { meterRegistry ->
            httpClientBuilder.setRequestExecutor(MicrometerHttpRequestExecutor.builder(meterRegistry).build())
            PoolingHttpClientConnectionManagerMetricsBinder(httpClientConnectionManager, name).bindTo(meterRegistry)
        }

        val httpClient: HttpClient = httpClientBuilder.build()
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = httpClient
        return requestFactory
    }
}