# Moya Android

Moya Android is a Kotlin networking library for Android 5.0 (API 21) and later. Define each API operation as a `TargetType` and execute it through a suspending `MoyaProvider`. The API follows the target and provider pattern from [Moya for iOS](https://moya.github.io/targets.html) and uses OkHttp for HTTP.

## Run the demo

Open this repository in Android Studio and run the `demo` configuration on an emulator or Android device. You can also install the debug build on a connected device with `./gradlew :demo:installDebug`, then open **Moya Demo** from the launcher.

Choose **Live API**, enter a public GitHub username, and tap **Send request**. The token field is optional. A Moya plugin adds an `Authorization: Bearer` header for live requests when you supply a token. The masked field keeps the token in memory for the screen session. See [GitHub authentication](https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api).

Choose **Encrypted offline stub** to load a bundled response for `octocat`. The service authenticates and decrypts the AES-GCM response before decoding its JSON into a typed profile. The fixture and its matching key ship together for demonstration. A live GitHub response arrives as JSON over HTTPS. See [Android cryptography](https://developer.android.com/privacy-and-security/cryptography).

- [ProfileScreen.kt](demo/src/main/kotlin/io/github/lyheangtaing/moya/demo/ui/ProfileScreen.kt) renders Jetpack Compose state and forwards user actions.
- [ProfileViewModel.kt](demo/src/main/kotlin/io/github/lyheangtaing/moya/demo/ui/ProfileViewModel.kt) exposes a `StateFlow` and runs requests in `viewModelScope`.
- [GitHubProfileService.kt](demo/src/main/kotlin/io/github/lyheangtaing/moya/demo/data/GitHubProfileService.kt) defines the Moya target, adds the bearer token, and selects live or stub mode.
- [ResponseDecryptor.kt](demo/src/main/kotlin/io/github/lyheangtaing/moya/demo/data/ResponseDecryptor.kt) authenticates and decrypts the bundled response.
- [GitHubProfile.kt](demo/src/main/kotlin/io/github/lyheangtaing/moya/demo/data/GitHubProfile.kt) defines the typed response and maps `public_repos` to a Kotlin property.

## Install from source

For an Android app in this Gradle build, add this to its `build.gradle.kts`.

```kotlin
dependencies {
    implementation(project(":moya"))
}
```

For a separate local Gradle build, publish the snapshot to Maven Local from this repository.

```sh
./gradlew :moya:publishReleasePublicationToMavenLocal
```

Add `mavenLocal()` to the consuming build's dependency repositories alongside `google()` and `mavenCentral()`. Then add the dependency to the app module.

```kotlin
dependencies {
    implementation("io.github.lyheangtaing:moya-android:0.1.0-SNAPSHOT")
}
```

The library manifest contributes Android's `INTERNET` permission to the app.

## Define and call an endpoint

```kotlin
import io.github.lyheangtaing.moya.MoyaProvider
import io.github.lyheangtaing.moya.TargetType
import io.github.lyheangtaing.moya.ValidationType
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

sealed interface GitHubApi : TargetType {
    override val baseUrl: HttpUrl get() = "https://api.github.com/".toHttpUrl()
    override val headers: Map<String, String> get() = mapOf("Accept" to "application/vnd.github+json")
    override val validation: ValidationType get() = ValidationType.Successful

    data class User(val username: String) : GitHubApi {
        override val pathSegments: List<String> get() = listOf("users", username)
        override val sampleData: ByteArray get() = """{"login":"octocat"}""".encodeToByteArray()
    }
}

class GitHubRepository(
    private val provider: MoyaProvider<GitHubApi> = MoyaProvider(),
) {
    suspend fun userJson(username: String): String =
        provider.request(GitHubApi.User(username)).bodyAsString()
}
```

Call the repository from a coroutine such as `viewModelScope.launch`. The provider buffers the response on an OkHttp worker thread and cancels the underlying call when the coroutine is cancelled. Reuse the provider across requests. Decode the returned text or bytes with your app's JSON library.

Path segments are raw strings and are encoded individually. A base URL path such as `/api/v1/` is preserved. To supply a request body or query parameters, override the target's `task`.

| Task | Use |
| --- | --- |
| `Task.Plain` | No parameters. The default for targets. |
| `Task.Query(listOf("page" to "2"))` | URL query parameters. Use repeated pairs for repeated keys. |
| `Task.json(json)` | A UTF-8 JSON string. Pair it with a method such as `Method.POST`. |
| `Task.Form(fields)` | URL-encoded form data. |
| `Task.Body(requestBody)` | An OkHttp `RequestBody`, including multipart uploads. |

JSON, form, and custom body tasks also accept query parameters.

## Status and errors

By default, the provider returns the HTTP response for any status code. Set a target's `validation` to `ValidationType.Successful` or call `response.filterSuccessfulStatusCodes()` to require a 2xx response. `MoyaException.HttpStatus` exposes the original response. `MoyaException.RequestMapping` identifies invalid request inputs and `MoyaException.Transport` wraps network and body-read I/O errors. Coroutine cancellation propagates as a `CancellationException`.

Responses are buffered in memory. Use `bodyAsString()` for text or `bodyBytes()` for binary data.

## Plugins and stubs

Pass `plugins = listOf(...)` to install `PluginType` hooks. Use `prepare` to modify a request, `willSend` to observe the prepared request, and `didReceive` to observe the response or error. Hooks run in the caller's coroutine context. If a hook throws, the exception propagates to the caller.

For offline tests, inject a provider with immediate stubs.

```kotlin
import io.github.lyheangtaing.moya.MoyaProvider
import io.github.lyheangtaing.moya.StubBehavior

val provider = MoyaProvider<GitHubApi>(stubBehavior = { StubBehavior.Immediate })
val repository = GitHubRepository(provider)
```

A stub defaults to HTTP 200 with the target's `sampleData`. Use `StubBehavior.Delayed(milliseconds)` to test slow responses or supply `stubResponse` to set the status, headers, and body. Stubs use the same plugin and status validation flow as live requests.

## Build and test

Use JDK 17 and Android SDK Platform 35. Set `ANDROID_HOME` to your Android SDK directory or open the repository in Android Studio so it can configure the SDK path.

```sh
./gradlew :moya:testDebugUnitTest :moya:lintRelease :moya:assembleRelease
```

Gradle writes the Android archive to `moya/build/outputs/aar/moya-release.aar`. Use the Gradle project or Maven dependency when integrating the SDK so OkHttp and coroutines resolve automatically.
