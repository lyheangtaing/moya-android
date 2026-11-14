package io.github.lyheangtaing.moya

import java.io.IOException

public sealed class MoyaException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    public class RequestMapping(cause: IllegalArgumentException) :
        MoyaException("Could not create the HTTP request", cause)

    public class Transport(cause: IOException) :
        MoyaException("HTTP request failed", cause)

    public class HttpStatus(public val response: MoyaResponse) :
        MoyaException("HTTP ${response.statusCode} did not pass validation")
}
