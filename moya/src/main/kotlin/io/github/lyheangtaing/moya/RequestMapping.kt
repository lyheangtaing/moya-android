package io.github.lyheangtaing.moya

import okhttp3.FormBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal fun TargetType.toRequest(): Request {
    val url = baseUrl.newBuilder()
    for (segment in pathSegments) {
        require(segment.isNotEmpty() && segment != "." && segment != "..") {
            "Path segments must be nonempty and cannot be dot segments"
        }
        url.addPathSegment(segment)
    }

    val endpointTask = task
    val query = when (endpointTask) {
        Task.Plain -> emptyList()
        is Task.Query -> endpointTask.parameters
        is Task.Form -> endpointTask.query
        is Task.Body -> endpointTask.query
    }
    for ((name, value) in query) {
        url.addQueryParameter(name, value)
    }

    val requestMethod = method
    val body = when (endpointTask) {
        Task.Plain, is Task.Query -> when (requestMethod) {
            Method.POST, Method.PUT, Method.PATCH -> byteArrayOf().toRequestBody()
            else -> null
        }
        is Task.Form -> FormBody.Builder().apply {
            for ((name, value) in endpointTask.fields) {
                add(name, value)
            }
        }.build()
        is Task.Body -> endpointTask.body
    }
    require(requestMethod != Method.TRACE || body == null) { "TRACE cannot have a request body" }

    return Request.Builder()
        .url(url.build())
        .method(requestMethod.name, body)
        .apply {
            for ((name, value) in headers) {
                header(name, value)
            }
        }
        .build()
}
