package io.github.lyheangtaing.moya

import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf

/** Controls whether a provider executes a live call or returns a fixture for a target. */
public sealed interface StubBehavior {
    public data object Never : StubBehavior
    public data object Immediate : StubBehavior
    public data class Delayed(val milliseconds: Long) : StubBehavior {
        init {
            require(milliseconds >= 0) { "Stub delay cannot be negative" }
        }
    }
}

public data class StubResponse(
    val statusCode: Int = 200,
    val data: ByteArray = byteArrayOf(),
    val headers: Headers = headersOf(),
)
