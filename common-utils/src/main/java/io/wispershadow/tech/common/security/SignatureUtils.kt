package io.wispershadow.tech.common.security

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

object SignatureUtils {
    private const val SIGNATURE_TYPE = "SHA256WithRSA"
    private const val KEY_TYPE = "RSA"
    private val logger: Logger = LoggerFactory.getLogger(SignatureUtils::class.java)

    init {
        try {
            val providerClassName =  "org.bouncycastle.jce.provider.BouncyCastleProvider"
            val providerClass = Class.forName(providerClassName)
            val provider  = providerClass.getConstructor().newInstance() as Provider
            Security.addProvider(
                provider
            )
            logger.info("BouncyCastleProvider loaded")
        }
        catch (e: Exception) {
            logger.info("Unable to load BouncyCastleProvider")
        }
    }


    /**
     * To Generate a key pair
     * openssl genrsa -out private.pem 2048
     * openssl rsa -in private.pem -outform PEM -pubout -out public.pem
     */
    fun generateSignature(payload: String,  keyString: String): String {
        val privateKey = generatePrivateKey(keyString)
        val sign = Signature.getInstance(SIGNATURE_TYPE)
        sign.initSign(privateKey)
        sign.update(payload.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(sign.sign())
    }

    fun verifySignature(payload: String, signature: String, keyString: String): Boolean {
        return verifySignature(payload.toByteArray(Charsets.UTF_8), signature, keyString)
    }

    fun verifySignature(payloadStream: InputStream, signature: String, keyString: String): Boolean {
        return verifySignature(payloadStream.readAllBytes(), signature, keyString)
    }

    fun verifySignature(payload: ByteArray, signature: String, keyString: String): Boolean {
        return try {
            val publicKey = generatePublicKey(keyString)
            val sign = Signature.getInstance(SIGNATURE_TYPE)
            sign.initVerify(publicKey)
            sign.update(payload)
            sign.verify(Base64.getDecoder().decode(signature))
        }
        catch (e: Exception) {
            logger.error("Error verifying signature ", e)
            false
        }
    }


    fun generatePrivateKey(keyString: String): PrivateKey {
        val keyBytes = keyString.toByteArray(Charsets.UTF_8)
        val kf = KeyFactory.getInstance(KEY_TYPE)
        val spec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyBytes))
        return kf.generatePrivate(spec)
    }

    fun generatePublicKey(keyString: String): PublicKey {
        val keyBytes = keyString.toByteArray(Charsets.UTF_8)
        val kf = KeyFactory.getInstance(KEY_TYPE)
        val x509EncodedKeySpec = X509EncodedKeySpec(Base64.getDecoder().decode(keyBytes))
        return kf.generatePublic(x509EncodedKeySpec)
    }
}