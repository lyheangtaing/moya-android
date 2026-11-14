package io.github.lyheangtaing.moya.demo

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import io.github.lyheangtaing.moya.MoyaException
import io.github.lyheangtaing.moya.MoyaProvider
import io.github.lyheangtaing.moya.StubBehavior
import io.github.lyheangtaing.moya.demo.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val liveProvider = MoyaProvider<GitHubUser>()
    private val stubProvider = MoyaProvider<GitHubUser>(stubBehavior = { StubBehavior.Immediate })
    private var requestJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        binding.sendRequest.setOnClickListener { sendRequest() }
        binding.username.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_GO) return@setOnEditorActionListener false
            sendRequest()
            true
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun sendRequest() {
        if (requestJob?.isActive == true) return

        val username = binding.username.text.toString().trim()
        binding.username.error = when {
            username.isEmpty() -> getString(R.string.username_required)
            !githubUsername.matches(username) -> getString(R.string.invalid_username)
            else -> null
        }
        if (binding.username.error != null) return

        val inputMethod = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethod.hideSoftInputFromWindow(binding.username.windowToken, 0)
        binding.username.clearFocus()

        val useStub = binding.stubMode.isChecked
        val provider = if (useStub) stubProvider else liveProvider
        showProfile(null)
        setLoading(true)
        showStatus(getString(R.string.loading), R.color.muted)

        requestJob = scope.launch {
            try {
                val response = provider.request(GitHubUser(username))
                val profile = GitHubProfile(JSONObject(response.bodyAsString()))
                val status = if (useStub) R.string.stub_status else R.string.live_status
                showStatus(getString(status, response.statusCode), R.color.accent)
                showProfile(profile)
            } catch (error: MoyaException.HttpStatus) {
                val code = error.response.statusCode
                showError(if (code == 404) getString(R.string.not_found) else getString(R.string.http_error, code))
            } catch (_: MoyaException.Transport) {
                showError(getString(R.string.network_error))
            } catch (_: MoyaException.RequestMapping) {
                showError(getString(R.string.invalid_request))
            } catch (_: JSONException) {
                showError(getString(R.string.invalid_response))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.sendRequest.isEnabled = !loading
        binding.sendRequest.alpha = if (loading) 0.55f else 1f
        binding.username.isEnabled = !loading
        binding.liveMode.isEnabled = !loading
        binding.stubMode.isEnabled = !loading
    }

    private fun showProfile(profile: GitHubProfile?) {
        val visibility = if (profile == null) View.GONE else View.VISIBLE
        binding.profileName.visibility = visibility
        binding.profileHandle.visibility = visibility
        binding.profileCounts.visibility = visibility
        if (profile == null) return

        binding.profileName.text = profile.name
        binding.profileHandle.text = getString(R.string.profile_handle, profile.login)
        binding.profileCounts.text = getString(R.string.profile_counts, profile.followers, profile.repositories)
    }

    private fun showError(message: String) = showStatus(message, R.color.error)

    @Suppress("DEPRECATION")
    private fun showStatus(message: String, color: Int) {
        binding.status.text = message
        binding.status.setTextColor(resources.getColor(color))
    }

    @Suppress("DEPRECATION")
    private fun applyWindowInsets() {
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safeArea = insets.getInsets(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime(),
                )
                view.setPadding(safeArea.left, safeArea.top, safeArea.right, safeArea.bottom)
            } else {
                view.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }
            insets
        }
    }

    private companion object {
        val githubUsername = Regex("[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*")
    }
}
