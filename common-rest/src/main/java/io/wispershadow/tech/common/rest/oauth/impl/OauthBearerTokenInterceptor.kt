package io.wispershadow.tech.common.rest.oauth.impl

import io.wispershadow.tech.common.rest.oauth.OauthToken
import io.wispershadow.tech.common.rest.oauth.OauthTokenManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

open class OauthBearerTokenInterceptor(
    val headerName: String,
    val oauthTokenManager: OauthTokenManager,
    val clientIdProvider: (HttpRequest, ByteArray) -> String,
    val tokenExpireResponseChecker: (ClientHttpResponse) -> Boolean = { true },
    val preSendCallback: (HttpRequest, ByteArray, String, OauthToken) -> Unit = { _, _, _, _ -> }
): ClientHttpRequestInterceptor {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OauthBearerTokenInterceptor::class.java)
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val clientId = clientIdProvider.invoke(request, body)
        val currentToken = oauthTokenManager.getOauthToken(clientId)
        val headers = request.headers
        if (currentToken != null) {
            headers.set(headerName, "Bearer ${currentToken.accessToken}")
            logger.info("Successfully setting auth header for clientId: {}", clientId)
            preSendCallback.invoke(request, body, clientId, currentToken)
        }
        val response = execution.execute(request, body)
        if (currentToken != null && tokenExpireResponseChecker.invoke(response)) {
            oauthTokenManager.invalidateOauthToken(clientId, currentToken)
        }
        return response
    }

}