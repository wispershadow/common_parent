package io.wispershadow.tech.common.rest.oauth.impl

import io.wispershadow.tech.common.rest.oauth.OauthToken

class DefaultOauthToken(
    override var acquiredTime: Long,
    override var expireInMillis: Long,
    override var accessToken: String
) : OauthToken {
}