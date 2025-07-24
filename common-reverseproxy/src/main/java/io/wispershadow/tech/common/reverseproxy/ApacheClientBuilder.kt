package io.wispershadow.tech.common.reverseproxy

import io.wispershadow.tech.common.reverseproxy.config.ApacheClientSettingProperties
import io.wispershadow.tech.common.reverseproxy.config.ReverseProxySettingProperties
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.io.HttpClientConnectionManager
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout
import java.util.concurrent.TimeUnit

object ApacheClientBuilder {
    private fun buildSocketConfig(clientSettingProperties: ApacheClientSettingProperties): SocketConfig {
        val readTimeout = clientSettingProperties.readTimeout
        if (readTimeout < 1) {
            throw RuntimeException("Read timeout must be greater than 0")
        }
        return SocketConfig.custom()
            .setSoTimeout(Timeout.of(readTimeout, TimeUnit.MILLISECONDS))
            .build()
    }


    private fun buildConnectionConfig(clientSettingProperties: ApacheClientSettingProperties): ConnectionConfig {
        return ConnectionConfig.custom()
            .setConnectTimeout(Timeout.of(clientSettingProperties.connectionTimeout, TimeUnit.MILLISECONDS))
            .setSocketTimeout(Timeout.of(clientSettingProperties.readTimeout, TimeUnit.MILLISECONDS))
            .setTimeToLive(Timeout.of(clientSettingProperties.connectionTimeToLive, TimeUnit.MILLISECONDS))
            .build()
    }

    private fun createConnectionManager(clientSettingProperties: ApacheClientSettingProperties): HttpClientConnectionManager {
        val manager = PoolingHttpClientConnectionManager()
        manager.maxTotal = clientSettingProperties.maxConnectionsTotal
        manager.defaultMaxPerRoute = clientSettingProperties.maxConnectionsPerRoute
        manager.defaultSocketConfig = buildSocketConfig(clientSettingProperties)
        manager.setDefaultConnectionConfig(buildConnectionConfig(clientSettingProperties))
        return manager
    }

    private fun buildRequestConfig(
        clientSettingProperties: ApacheClientSettingProperties,
        reverseProxySettingProperties: ReverseProxySettingProperties
    ): RequestConfig {
        return RequestConfig.custom()
            .setRedirectsEnabled(reverseProxySettingProperties.handleRedirects)
            .setCookieSpec(StandardCookieSpec.IGNORE) // we handle them in the servlet instead
            .build()
    }

    fun createHttpClient(
        clientSettingProperties: ApacheClientSettingProperties,
        reverseProxySettingProperties: ReverseProxySettingProperties
    ): HttpClient {
        val clientBuilder = HttpClientBuilder.create()
        clientBuilder.setConnectionManager(createConnectionManager(clientSettingProperties))
        clientBuilder.setDefaultRequestConfig(
            buildRequestConfig(
                clientSettingProperties,
                reverseProxySettingProperties
            )
        )
        return clientBuilder.build()
    }
}