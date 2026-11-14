package io.github.lyheangtaing.moya

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody

/** A fully buffered HTTP response. [request] is the request associated with this response. */
public class MoyaResponse internal constructor(
    public val statusCode: Int,
    public val headers: Headers,
    public val request: Request,
    private val data: ByteArray,
) {
    public val isSuccessful: Boolean get() = statusCode in 200..299

    public fun bodyBytes(): ByteArray = data.copyOf()

    /** Uses the body byte order mark or declared charset, falling back to UTF-8. */
    public fun bodyAsString(): String =
        data.toResponseBody(headers["Content-Type"]?.toMediaTypeOrNull()).string()

    /** Returns this response or throws with the original status, headers, and body. */
    public fun filterSuccessfulStatusCodes(): MoyaResponse {
        if (!isSuccessful) throw MoyaException.HttpStatus(this)
        return this
    }
}
