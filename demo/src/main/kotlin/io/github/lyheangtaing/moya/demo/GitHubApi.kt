package io.github.lyheangtaing.moya.demo

import io.github.lyheangtaing.moya.TargetType
import io.github.lyheangtaing.moya.ValidationType
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

internal data class GitHubUser(val username: String) : TargetType {
    override val baseUrl get() = apiUrl
    override val pathSegments get() = listOf("users", username)
    override val headers get() = mapOf("Accept" to "application/vnd.github+json")
    override val validation get() = ValidationType.Successful
    override val sampleData: ByteArray
        get() = JSONObject()
            .put("login", username)
            .put("name", "Sample GitHub User")
            .put("followers", 128)
            .put("public_repos", 12)
            .toString()
            .encodeToByteArray()

    private companion object {
        val apiUrl = "https://api.github.com/".toHttpUrl()
    }
}

internal class GitHubProfile(json: JSONObject) {
    val login: String = json.getString("login")
    val name: String = if (json.isNull("name")) login else json.getString("name").ifBlank { login }
    val followers: Int = json.getInt("followers")
    val repositories: Int = json.getInt("public_repos")
}
