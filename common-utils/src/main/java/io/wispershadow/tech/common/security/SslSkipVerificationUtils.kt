package io.wispershadow.tech.common.security

import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object SslSkipVerificationUtils {
    fun ignoreSsl() {
        val hostNameVerifier = HostnameVerifier { hostname, session -> true }
        trustAllHttpsCertificates()
        HttpsURLConnection.setDefaultHostnameVerifier(hostNameVerifier)
    }

    fun trustAllHttpsCertificates() {
        val trustAllCerts = arrayOf(TrustAllTrustManager())
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, null)
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
    }

    class TrustAllTrustManager: TrustManager, X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }


        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }

    }
}