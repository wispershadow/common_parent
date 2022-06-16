package io.wispershadow.tech.common.rest.oauth.impl

import io.wispershadow.tech.common.rest.oauth.OauthToken
import io.wispershadow.tech.common.rest.oauth.OauthTokenAccqConfigData
import io.wispershadow.tech.common.rest.oauth.OauthTokenContext
import io.wispershadow.tech.common.rest.oauth.OauthTokenManager

abstract class AbstractOauthTokenManager(val oauthTokenAccqConfigData: OauthTokenAccqConfigData): OauthTokenManager  {
    private val oauthTokenContext = OauthTokenContext(oauthTokenAccqConfigData.tokenExpireThresholdInSeconds)

    override fun getOauthToken(clientId: String): OauthToken? {
        return oauthTokenContext.getToken(clientId) { clientId ->
            acquireOauthToken(clientId)
        }
    }

    override fun invalidateOauthToken(clientId: String, token: OauthToken) {
        oauthTokenContext.compareAndRemoveToken(clientId, token)
    }

    abstract fun acquireOauthToken(clientId: String): OauthToken

    fun destroy() {
        oauthTokenContext.clear()
    }

}