package io.github.lyheangtaing.moya.demo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.lyheangtaing.moya.demo.R
import io.github.lyheangtaing.moya.demo.data.RequestMode

private val colors = lightColorScheme(
    primary = Color(0xFF1F5A43),
    onPrimary = Color.White,
    background = Color(0xFFF3F5F0),
    onBackground = Color(0xFF192721),
    surface = Color.White,
    onSurface = Color(0xFF192721),
    onSurfaceVariant = Color(0xFF46574E),
    outline = Color(0xFF697B70),
)

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    onUsernameChange: (String) -> Unit,
    onModeChange: (RequestMode) -> Unit,
    onTokenChange: () -> Unit,
    onRequest: (String) -> Unit,
) {
    // A token belongs to the current screen session and is never written to saved state.
    var token by remember { mutableStateOf("") }
    val loading = state.result == ProfileResult.Loading
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onRequest(token)
    }

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.title), style = MaterialTheme.typography.headlineLarge)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.request_mode), style = MaterialTheme.typography.titleSmall)
                            Column(Modifier.selectableGroup()) {
                                ModeOption(R.string.live_mode, state.mode == RequestMode.Live, !loading) {
                                    onModeChange(RequestMode.Live)
                                }
                                ModeOption(R.string.stub_mode, state.mode == RequestMode.EncryptedStub, !loading) {
                                    onModeChange(RequestMode.EncryptedStub)
                                }
                            }
                            if (state.mode == RequestMode.Live) {
                                OutlinedTextField(
                                    value = state.username,
                                    onValueChange = { if (it.length <= 39) onUsernameChange(it) },
                                    label = { Text(stringResource(R.string.username)) },
                                    enabled = !loading,
                                    isError = state.usernameError != null,
                                    supportingText = state.usernameError?.let { id -> { Text(stringResource(id)) } },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                    keyboardActions = KeyboardActions(onGo = { submit() }),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = token,
                                    onValueChange = {
                                        token = it
                                        onTokenChange()
                                    },
                                    label = { Text(stringResource(R.string.token_label)) },
                                    enabled = !loading,
                                    isError = state.tokenError != null,
                                    supportingText = {
                                        Text(stringResource(state.tokenError ?: R.string.token_help))
                                    },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Go,
                                    ),
                                    keyboardActions = KeyboardActions(onGo = { submit() }),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Text(stringResource(R.string.stub_help), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                text = stringResource(R.string.request_preview),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(onClick = submit, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.send_request))
                            }
                        }
                    }
                    ResponseCard(state.result)
                }
            }
        }
    }
}

@Composable
private fun ModeOption(label: Int, selected: Boolean, enabled: Boolean, select: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = select)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(stringResource(label))
    }
}

@Composable
private fun ResponseCard(result: ProfileResult) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.response_title), style = MaterialTheme.typography.titleMedium)
            when (result) {
                ProfileResult.Idle -> Text(stringResource(R.string.empty_response))
                ProfileResult.Loading -> {
                    Text(stringResource(R.string.loading))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is ProfileResult.Error -> {
                    val text = result.statusCode?.let { stringResource(result.message, it) }
                        ?: stringResource(result.message)
                    Text(text, color = MaterialTheme.colorScheme.error)
                }
                is ProfileResult.Success -> {
                    val (profile, code) = result.response
                    val status = if (result.mode == RequestMode.Live) R.string.live_status else R.string.stub_status
                    Text(stringResource(status, code), color = MaterialTheme.colorScheme.primary)
                    Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.profile_handle, profile.login))
                    Text(stringResource(R.string.profile_counts, profile.followers, profile.publicRepositories))
                }
            }
        }
    }
}
