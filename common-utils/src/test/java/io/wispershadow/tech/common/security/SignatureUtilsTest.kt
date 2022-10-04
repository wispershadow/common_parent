package io.wispershadow.tech.common.security

import io.wispershadow.tech.common.utils.DataLoadUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SignatureUtilsTest {
    @Test
    fun testGenerateVerifySignature() {
        val privateKeyStr = DataLoadUtils.loadTestDataAsLines("signature/privatekey_test").joinToString("")
        val publicKeyStr = DataLoadUtils.loadTestDataAsLines("signature/publickey_test").joinToString("")
        val payloadStrList = DataLoadUtils.loadTestDataAsLines("signature/testpayload.json")
        payloadStrList.forEach { payloadStr ->
            val signature = SignatureUtils.generateSignature(payloadStr, privateKeyStr)
            val verificationResult = SignatureUtils.verifySignature(payloadStr, signature, publicKeyStr)
            Assertions.assertTrue(verificationResult)
        }
    }




}