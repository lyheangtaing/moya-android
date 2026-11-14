package io.github.lyheangtaing.moya.demo.data

import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GitHubProfileServiceTest {
    private val fixture = File("src/main/res/raw/encrypted_profile.json").readBytes()

    @Test
    fun liveResponseAttachesTokenAndMapsJson() = runBlocking {
        var authorization: String? = null
        val service = GitHubProfileService(
            encryptedFixture = { fixture },
            callFactory = OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                authorization = request.header("Authorization")
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"login":"octocat","name":null,"followers":8,"public_repos":2,"id":42}""".toResponseBody())
                    .build()
            }.build(),
        )

        val result = service.fetchProfile("octocat", RequestMode.Live, "demo-token")

        assertEquals("Bearer demo-token", authorization)
        assertEquals(200, result.statusCode)
        assertEquals("octocat", result.profile.displayName)
        assertEquals(2, result.profile.publicRepositories)

        service.fetchProfile("octocat", RequestMode.Live, "")
        assertNull(authorization)
    }

    @Test
    fun encryptedStubAuthenticatesAndDecodesWithoutNetwork() = runBlocking {
        val service = GitHubProfileService(
            encryptedFixture = { fixture },
            callFactory = Call.Factory { error("An offline stub must never start a network call") },
        )

        val result = service.fetchProfile("octocat", RequestMode.EncryptedStub)

        assertEquals(200, result.statusCode)
        assertEquals("octocat", result.profile.login)
        assertEquals("Sample GitHub User", result.profile.displayName)
        assertEquals(128, result.profile.followers)
    }

    @Test
    fun tamperedCiphertextFailsAuthentication() {
        val envelope = fixture.decodeToString().trimEnd()
        val index = envelope.indexOf("\"ciphertext\":\"") + "\"ciphertext\":\"".length
        val swappedDigit = if (envelope[index] == '0') "1" else "0"
        val tampered = envelope.replaceRange(index, index + 1, swappedDigit)
        val service = GitHubProfileService(encryptedFixture = { tampered.encodeToByteArray() })

        assertThrows(ResponseDecryptionException::class.java) {
            runBlocking { service.fetchProfile("octocat", RequestMode.EncryptedStub) }
        }
    }
}
