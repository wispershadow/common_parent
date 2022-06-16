package io.wispershadow.tech.common.rest.oauth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.ReentrantReadWriteLock

class OauthTokenContext(private val tokenExpireThresholdInSeconds: Long) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OauthTokenContext::class.java)
    }
    private val readWriteLock = ReentrantReadWriteLock()
    private val readLock = readWriteLock.readLock()
    private val writeLock = readWriteLock.writeLock()
    private val clientAppOauthTokens = mutableMapOf<String, OauthToken>()

    fun addToken(clientId: String, oauthToken: OauthToken) {
        writeLock.lockInterruptibly()
        try {
            clientAppOauthTokens[clientId] = oauthToken
        }
        finally {
            writeLock.unlock()
        }
    }

    fun removeToken(clientId: String): OauthToken? {
        writeLock.lockInterruptibly()
        try {
            return clientAppOauthTokens.remove(clientId)
        }
        finally {
            writeLock.unlock()
        }
    }

    fun compareAndRemoveToken(clientId: String, currentToken: OauthToken): Boolean {
        readLock.lock()
        val loaded1 = clientAppOauthTokens[clientId]
        var tokenValid = compareToken(loaded1, currentToken)
        if (tokenValid) {
            readLock.unlock()
            writeLock.lock()
            try {
                val loaded2 = clientAppOauthTokens[clientId]
                tokenValid = compareToken(loaded2, currentToken)
                if (tokenValid) {
                    logger.info("Successfully removed token: {}", currentToken)
                    clientAppOauthTokens.remove(clientId)
                }
                readLock.lock()
            }
            finally {
                writeLock.unlock()
            }
        }
        try {
            logger.info("Complete compare and remove token, tokenValid: {}", tokenValid)
            return tokenValid
        }
        finally {
            readLock.unlock()
        }
    }

    fun clear() {
        writeLock.lockInterruptibly()
        try {
            clientAppOauthTokens.clear()
        }
        finally {
            writeLock.unlock()
        }

    }

    fun getToken(clientId: String, tokenLoaderFun: (String)-> OauthToken): OauthToken? {
        readLock.lock()
        val currentToken = clientAppOauthTokens[clientId]
        val cacheValid = isTokenValid(currentToken)
        if (!cacheValid) {
            readLock.unlock()
            writeLock.lock()
            try {
                val currentTokenRefresh = clientAppOauthTokens[clientId]
                if (!isTokenValid(currentTokenRefresh)) {
                    logger.info("Try to load new token for clientId: {}", clientId)
                    val newToken = tokenLoaderFun.invoke(clientId)
                    addToken(clientId, newToken)
                    logger.info("New token for clientId: {} acquired, value is: {}", clientId, newToken)
                }
                readLock.lock()
            }
            finally {
                writeLock.unlock()
            }
        }
        try {
            return clientAppOauthTokens[clientId]
        }
        finally {
            readLock.unlock()
        }

    }

    private fun isTokenValid(oauthToken: OauthToken?): Boolean {
        if (oauthToken == null) {
            return false
        }
        return !oauthToken.isExpiring(tokenExpireThresholdInSeconds).also {
            if (it) {
                logger.info("Oauth token: {} is expiring", oauthToken)
            }
        }
    }

    private fun compareToken(loadedToken: OauthToken?, existingToken: OauthToken): Boolean {
        if (loadedToken == null) {
            return false
        }
        return loadedToken.accessToken == existingToken.accessToken
    }
}