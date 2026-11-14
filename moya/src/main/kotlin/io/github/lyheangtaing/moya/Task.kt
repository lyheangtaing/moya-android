package io.github.lyheangtaing.moya

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Describes how parameters and request data are sent. */
public sealed interface Task {
    public data object Plain : Task

    /** Pairs retain ordering and repeated keys. A null value sends a name without a value. */
    public data class Query(val parameters: List<Pair<String, String?>>) : Task

    public data class Form(
        val fields: List<Pair<String, String>>,
        val query: List<Pair<String, String?>> = emptyList(),
    ) : Task

    /** Accepts any OkHttp body, including multipart, files, and custom encodings. */
    public data class Body(
        val body: RequestBody,
        val query: List<Pair<String, String?>> = emptyList(),
    ) : Task

    public companion object {
        /** Sends pre-encoded JSON as UTF-8. */
        public fun json(json: String, query: List<Pair<String, String?>> = emptyList()): Body =
            Body(json.toRequestBody("application/json; charset=utf-8".toMediaType()), query)
    }
}
