package io.github.lyheangtaing.moya.demo.data

import io.github.lyheangtaing.moya.MoyaProvider
import io.github.lyheangtaing.moya.PluginType
import io.github.lyheangtaing.moya.StubBehavior
import io.github.lyheangtaing.moya.StubResponse
import io.github.lyheangtaing.moya.TargetType
import io.github.lyheangtaing.moya.ValidationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class GitHubProfileService(
    encryptedFixture: () -> ByteArray,
    callFactory: Call.Factory = OkHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    // This public key is paired with the bundled fixture and is for the demo only.
    private val decryptor = ResponseDecryptor("634fa061198fcad1e2a1b00955f39a52".hexToByteArray())
    private val provider = MoyaProvider<GitHubUserTarget>(
        callFactory = callFactory,
        plugins = listOf(BearerTokenPlugin),
        stubBehavior = { target ->
            if (target.mode == RequestMode.EncryptedStub) StubBehavior.Immediate else StubBehavior.Never
        },
        stubResponse = { StubResponse(data = encryptedFixture()) },
    )

    suspend fun fetchProfile(username: String, mode: RequestMode, token: String = ""): ProfileResponse =
        withContext(Dispatchers.IO) {
            val liveToken = token.takeIf { mode == RequestMode.Live && it.isNotBlank() }
            val response = provider.request(GitHubUserTarget(username, mode, liveToken))
            val responseBody = response.bodyAsString()
            val profileJson = when (mode) {
                RequestMode.Live -> responseBody
                RequestMode.EncryptedStub -> decryptor.decrypt(responseBody).decodeToString(throwOnInvalidSequence = true)
            }
            ProfileResponse(json.decodeFromString<GitHubProfile>(profileJson), response.statusCode)
        }

    private class GitHubUserTarget(
        username: String,
        val mode: RequestMode,
        val token: String?,
    ) : TargetType {
        override val baseUrl = githubApi
        override val pathSegments = listOf("users", username)
        override val headers = mapOf("Accept" to "application/vnd.github+json")
        override val validation = ValidationType.Successful
    }

    private object BearerTokenPlugin : PluginType {
        override fun prepare(request: Request, target: TargetType): Request {
            val token = (target as GitHubUserTarget).token ?: return request
            return request.newBuilder().header("Authorization", "Bearer $token").build()
        }
    }

    private companion object {
        val githubApi = "https://api.github.com/".toHttpUrl()
    }
}
