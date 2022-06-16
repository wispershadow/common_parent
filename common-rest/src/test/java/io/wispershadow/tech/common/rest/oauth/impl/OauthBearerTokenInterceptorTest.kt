package io.wispershadow.tech.common.rest.oauth.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.wispershadow.tech.common.rest.oauth.OauthToken
import io.wispershadow.tech.common.rest.oauth.OauthTokenAccqConfigData
import io.wispershadow.tech.common.rest.oauth.OauthTokenManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution

class OauthBearerTokenInterceptorTest {
    @Test
    fun testHeaderAdded() {
        val oauthTokenAccqConfigData = OauthTokenAccqConfigData()
        val oauthTokenManager = TestOauthTokenManager(oauthTokenAccqConfigData)
        val clientId = "testClient1"
        val oauthBearerTokenInterceptor = OauthBearerTokenInterceptor(oauthTokenAccqConfigData.oauthTokenHeaderName,
            oauthTokenManager, {_ , _ -> clientId})
        val headerDataMap = mutableMapOf<String, String>()
        val httpRequest = buildHttpRequest(headerDataMap)
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)
        oauthBearerTokenInterceptor.intercept(httpRequest, ByteArray(10), execution)
        Assertions.assertEquals(headerDataMap, mapOf("Authorization" to "Bearer abc123"))
    }

    @Test
    fun testMissingHeader() {
        val clientId = "testClient1"
        val oauthTokenManager = mockk<OauthTokenManager>(relaxed = true)
        every {oauthTokenManager.getOauthToken(any())} returns null
        val oauthBearerTokenInterceptor = OauthBearerTokenInterceptor("Authorization",
            oauthTokenManager, {_ , _ -> clientId})
        val headerDataMap = mutableMapOf<String, String>()
        val httpRequest = buildHttpRequest(headerDataMap)
        val execution = mockk<ClientHttpRequestExecution>(relaxed = true)
        oauthBearerTokenInterceptor.intercept(httpRequest, ByteArray(10), execution)
        Assertions.assertEquals(headerDataMap, emptyMap<String, String>())
        verify(exactly = 0) {  oauthTokenManager.invalidateOauthToken(any(), any()) }
    }

    private fun buildHttpRequest(headerDataMap: MutableMap<String, String>): HttpRequest {
        val httpRequest = mockk<HttpRequest>(relaxed = true)
        val headers = mockk<HttpHeaders>(relaxed = true)
        every { httpRequest.headers } returns headers
        every { headers.set(any(), any())} answers { scope ->
            val headerName = scope.invocation.args[0] as String
            val headerValue = scope.invocation.args[1] as String
            headerDataMap[headerName] = headerValue
        }
        return httpRequest
    }

    class TestOauthTokenManager(oauthTokenAccqConfigData: OauthTokenAccqConfigData): AbstractOauthTokenManager(oauthTokenAccqConfigData) {
        override fun acquireOauthToken(clientId: String): OauthToken {
            return DefaultOauthToken(
                System.currentTimeMillis(),
                8 * 1000,
                "abc123"
            )
        }
    }
}