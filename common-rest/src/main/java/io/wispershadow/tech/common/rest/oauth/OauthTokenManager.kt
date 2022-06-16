package io.wispershadow.tech.common.rest.oauth

/**
 * Responsible for retrieving token from context, if
 * token is not available, it will retrieve token by invoking the load function
 */
interface OauthTokenManager {
    fun getOauthToken(clientId: String): OauthToken?

    fun invalidateOauthToken(clientId: String, token: OauthToken)
}