package io.github.lyheangtaing.moya.demo.data

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class ResponseDecryptor(key: ByteArray) {
    private val secretKey = SecretKeySpec(key, "AES")

    fun decrypt(body: String): ByteArray {
        try {
            val envelope = Json.decodeFromString<EncryptedResponse>(body)
            val nonce = envelope.iv.hexToByteArray()
            val ciphertext = envelope.ciphertext.hexToByteArray()
            require(nonce.size == 12 && ciphertext.size >= 16) { "Invalid AES-GCM envelope" }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, nonce))
            return cipher.doFinal(ciphertext)
        } catch (error: GeneralSecurityException) {
            throw ResponseDecryptionException(error)
        } catch (error: IllegalArgumentException) {
            throw ResponseDecryptionException(error)
        }
    }

    @Serializable
    private data class EncryptedResponse(val iv: String, val ciphertext: String)
}

internal class ResponseDecryptionException(cause: Throwable) :
    Exception("Could not authenticate and decrypt the response", cause)
