package io.github.lyheangtaing.moya.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.lyheangtaing.moya.demo.ui.ProfileScreen
import io.github.lyheangtaing.moya.demo.ui.ProfileViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<ProfileViewModel>()

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
