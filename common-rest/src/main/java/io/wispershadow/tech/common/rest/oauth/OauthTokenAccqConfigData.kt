package io.wispershadow.tech.common.rest.oauth

open class OauthTokenAccqConfigData {
    var oauthTokenHeaderName: String = "Authorization"
    var tokenExpireThresholdInSeconds: Long = 120
    var clientIdMappings: List<ClientIdMappingData> = emptyList()
}

open class ClientIdMappingData {
    lateinit var clientId: String
    var applicableBeanNames: List<String> = emptyList()
}