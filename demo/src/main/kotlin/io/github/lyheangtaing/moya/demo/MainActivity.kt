package io.github.lyheangtaing.moya.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.lyheangtaing.moya.demo.data.GitHubProfileService
import io.github.lyheangtaing.moya.demo.ui.ProfileScreen
import io.github.lyheangtaing.moya.demo.ui.ProfileViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ProfileViewModel> {
        val app = application
        viewModelFactory {
            initializer {
                ProfileViewModel(GitHubProfileService(encryptedFixture = {
                    app.resources.openRawResource(R.raw.encrypted_profile).use { it.readBytes() }
                }))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfileScreen(
                state = viewModel.state.collectAsStateWithLifecycle().value,
                onUsernameChange = viewModel::changeUsername,
                onModeChange = viewModel::changeMode,
                onTokenChange = viewModel::clearTokenError,
                onRequest = viewModel::fetchProfile,
            )
        }
    }
}
