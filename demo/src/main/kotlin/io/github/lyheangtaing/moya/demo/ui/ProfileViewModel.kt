package io.github.lyheangtaing.moya.demo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.lyheangtaing.moya.MoyaException
import io.github.lyheangtaing.moya.demo.R
import io.github.lyheangtaing.moya.demo.data.GitHubProfileService
import io.github.lyheangtaing.moya.demo.data.ProfileResponse
import io.github.lyheangtaing.moya.demo.data.RequestMode
import io.github.lyheangtaing.moya.demo.data.ResponseDecryptionException
import java.io.IOException
import java.nio.charset.CharacterCodingException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException

internal data class ProfileUiState(
    val username: String = "octocat",
    val mode: RequestMode = RequestMode.Live,
    val usernameError: Int? = null,
    val tokenError: Int? = null,
    val result: ProfileResult = ProfileResult.Idle,
)

internal sealed interface ProfileResult {
    data object Idle : ProfileResult
    data object Loading : ProfileResult
    data class Success(val response: ProfileResponse, val mode: RequestMode) : ProfileResult
    data class Error(val message: Int, val statusCode: Int? = null) : ProfileResult
}

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val service: GitHubProfileService,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state = mutableState.asStateFlow()

    fun changeUsername(username: String) {
        mutableState.update { it.copy(username = username, usernameError = null) }
    }

    fun changeMode(mode: RequestMode) {
        mutableState.update { it.copy(mode = mode, usernameError = null, tokenError = null, result = ProfileResult.Idle) }
    }

    fun clearTokenError() {
        mutableState.update { it.copy(tokenError = null) }
    }

    fun fetchProfile(token: String) {
        val current = mutableState.value
        if (current.result == ProfileResult.Loading) return

        val live = current.mode == RequestMode.Live
        val username = if (live) current.username.trim() else "octocat"
        val liveToken = if (live) token.trim() else ""
        val usernameError = when {
            username.isEmpty() -> R.string.username_required
            !githubUsername.matches(username) -> R.string.invalid_username
            else -> null
        }
        val tokenError = if (liveToken.isNotEmpty() && !bearerToken.matches(liveToken)) {
            R.string.invalid_token
        } else {
            null
        }
        if (usernameError != null || tokenError != null) {
            mutableState.update { it.copy(usernameError = usernameError, tokenError = tokenError) }
            return
        }

        mutableState.update { it.copy(result = ProfileResult.Loading) }
        viewModelScope.launch {
            val result = try {
                ProfileResult.Success(service.fetchProfile(username, current.mode, liveToken), current.mode)
            } catch (error: MoyaException.HttpStatus) {
                val code = error.response.statusCode
                when (code) {
                    401 -> ProfileResult.Error(R.string.unauthorized)
                    404 -> ProfileResult.Error(R.string.not_found)
                    else -> ProfileResult.Error(R.string.http_error, code)
                }
            } catch (_: MoyaException.Transport) {
                ProfileResult.Error(R.string.network_error)
            } catch (_: MoyaException.RequestMapping) {
                ProfileResult.Error(R.string.invalid_request)
            } catch (_: ResponseDecryptionException) {
                ProfileResult.Error(R.string.decryption_error)
            } catch (_: CharacterCodingException) {
                ProfileResult.Error(R.string.invalid_response)
            } catch (_: SerializationException) {
                ProfileResult.Error(R.string.invalid_response)
            } catch (_: IllegalArgumentException) {
                ProfileResult.Error(R.string.invalid_response)
            } catch (_: IOException) {
                ProfileResult.Error(R.string.invalid_response)
            }

            mutableState.update { it.copy(result = result) }
        }
    }

    private companion object {
        val githubUsername = Regex("(?=.{1,39}$)[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")
        val bearerToken = Regex("[A-Za-z0-9._~+/-]+=*")
    }
}
