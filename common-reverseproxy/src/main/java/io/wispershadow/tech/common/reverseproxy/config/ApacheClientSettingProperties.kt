package io.wispershadow.tech.common.reverseproxy.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "config.wispershadow.common.apacheclient")
class ApacheClientSettingProperties {
    var connectionTimeout: Long = -1L
    var readTimeout: Long = -1L
    var connectionTimeToLive: Long = -1L
    var maxConnectionsTotal = 50
    var maxConnectionsPerRoute = 10
}