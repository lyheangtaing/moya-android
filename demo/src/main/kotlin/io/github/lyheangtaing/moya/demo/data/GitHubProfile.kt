package io.github.lyheangtaing.moya.demo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GitHubProfile(
    val login: String,
    val name: String? = null,
    val followers: Int,
    @SerialName("public_repos") val publicRepositories: Int,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: login
}

internal data class ProfileResponse(val profile: GitHubProfile, val statusCode: Int)

internal enum class RequestMode {
    Live,
    EncryptedStub,
}
