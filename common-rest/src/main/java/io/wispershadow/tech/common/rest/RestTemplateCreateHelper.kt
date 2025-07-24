package io.wispershadow.tech.common.rest

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.wispershadow.tech.common.http.HttpClientConfig
import org.apache.hc.client5.http.HttpRequestRetryStrategy
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.socket.ConnectionSocketFactory
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.core5.http.config.Registry
import org.apache.hc.core5.http.config.RegistryBuilder
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.ssl.TrustStrategy
import org.apache.hc.core5.util.TimeValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.client.RestTemplateCustomizer
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.*
import java.util.function.Supplier

object RestTemplateCreateHelper {
    private val logger: Logger = LoggerFactory.getLogger(RestTemplateCreateHelper::class.java)

    fun createRestTemplate(restTemplateBuilder: RestTemplateBuilder,
                           connectionConfig: HttpClientConfig,
                           objectMapper: ObjectMapper,
                           name: String,
                           meterRegistryOptional: Optional<MeterRegistry>
    ): RestTemplate {
        return restTemplateBuilder.connectTimeout(
            Duration.ofMillis(connectionConfig.connectionTimeoutMillis)
        )
            .readTimeout(Duration.ofMillis(connectionConfig.readTimeoutMillis))
            .requestFactory(
                Supplier{ buildClientHttpRequestFactory(connectionConfig, name, meterRegistryOptional) }
            )
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

    private fun buildRetryStrategy(connectionConfig: HttpClientConfig): HttpRequestRetryStrategy {
        return DefaultHttpRequestRetryStrategy(connectionConfig.maxRetryTimes,
            TimeValue.ofMilliseconds(connectionConfig.retryInterval))

    }

    private fun buildClientHttpRequestFactory(
        connectionConfig: HttpClientConfig,
        name: String,
        meterRegistryOptional: Optional<MeterRegistry>,
    ): ClientHttpRequestFactory {
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
        val socketFactoryRegistry: Registry<ConnectionSocketFactory> = RegistryBuilder
            .create<ConnectionSocketFactory>().register("https", sslsf)
            .register("http", PlainConnectionSocketFactory())
            .build()
        val httpClientConnectionManager = PoolingHttpClientConnectionManager(socketFactoryRegistry).apply {
            this.defaultMaxPerRoute = connectionConfig.maxConnectionPerRoute
            this.maxTotal = connectionConfig.maxTotal
        }

        val httpClientBuilder = HttpClients.custom().setConnectionManager(httpClientConnectionManager)
            .setRetryStrategy(buildRetryStrategy(connectionConfig))


        val httpClient: HttpClient = httpClientBuilder.build()
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = httpClient
        return requestFactory
    }
}