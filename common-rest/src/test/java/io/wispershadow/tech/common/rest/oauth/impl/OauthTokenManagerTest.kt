package io.wispershadow.tech.common.rest.oauth.impl

import io.wispershadow.tech.common.rest.oauth.OauthToken
import io.wispershadow.tech.common.rest.oauth.OauthTokenAccqConfigData
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

class OauthTokenManagerTest {
    val oauthTokenAccqConfigData = OauthTokenAccqConfigData().apply {
        this.oauthTokenHeaderName = "Authorization"
        this.tokenExpireThresholdInSeconds = 5
    }
    val oauthTokenManager = TestOauthTokenManager(oauthTokenAccqConfigData)

    @Test
    fun testAcquireTokenTwiceNoExpiration() {
        val clientId = "clientApp1"
        val token1 = oauthTokenManager.getOauthToken(clientId)
        val token2 = oauthTokenManager.getOauthToken(clientId)
        Assertions.assertEquals(token1?.accessToken, token2?.accessToken)
        Assertions.assertEquals(oauthTokenManager.invokeCounter.get(), 1)
    }

    @Test
    fun testAcquireTokenMultiThread() {
        val numberOfThreads = 5
        val clientId = "clientApp1"
        val tokenSet = ConcurrentSkipListSet<String>()
        val futureList = (1 .. numberOfThreads).map {
            CompletableFuture.runAsync {
                Thread.sleep((3000 * Math.random()).toLong())
                val token = oauthTokenManager.getOauthToken(clientId)
                token?.let {
                    tokenSet.add(it.accessToken)
                }
            }
        }
        val allFutures = CompletableFuture.allOf(* futureList.toTypedArray())
        allFutures.get()
        Assertions.assertEquals(tokenSet.size, 1)
        Assertions.assertEquals(oauthTokenManager.invokeCounter.get(), 1)
    }

    @Test
    fun testAcquireTokenTwiceExpiration() {
        val clientId = "clientApp1"
        val token1 = oauthTokenManager.getOauthToken(clientId)
        Thread.sleep(4000)
        val token2 = oauthTokenManager.getOauthToken(clientId)
        Assertions.assertNotEquals(token1?.accessToken, token2?.accessToken)
        Assertions.assertEquals(oauthTokenManager.invokeCounter.get(), 2)
    }

    class TestOauthTokenManager(oauthTokenAccqConfigData: OauthTokenAccqConfigData): AbstractOauthTokenManager(oauthTokenAccqConfigData) {
        val invokeCounter: AtomicInteger = AtomicInteger(0)
        override fun acquireOauthToken(clientId: String): OauthToken {
            invokeCounter.incrementAndGet()
            return DefaultOauthToken(System.currentTimeMillis(), 8 * 1000, UUID.randomUUID().toString())
        }
    }
}

