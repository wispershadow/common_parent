package io.wispershadow.tech.common.reverseproxy.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "config.wispershadow.common.reverseproxy")
class ReverseProxySettingProperties {
    lateinit var sourceUriPrefix: String
    lateinit var targetUriRoot: String

    var forwardIP: Boolean  = false
    var preserveHost: Boolean = false
    var preserveCookiePath: Boolean = false
    var handleCompression: Boolean = false
    var handleRedirects: Boolean = false
}