package io.wispershadow.tech.common.rest.oauth

interface OauthToken {
    var acquiredTime: Long
    // number of milliseconds the token will expire
    var expireInMillis: Long
    var accessToken: String

    fun isExpiring(tokenExpireThresholdInSeconds: Long): Boolean  {
        val currentTime = System.currentTimeMillis()
        val timeDiff = expireInMillis - (currentTime - acquiredTime)
        return timeDiff <= tokenExpireThresholdInSeconds * 1000
    }
}