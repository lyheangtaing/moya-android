package io.github.lyheangtaing.moya

import okhttp3.HttpUrl

/** Describes one API operation. A sealed interface can group related operations. */
public interface TargetType {
    public val baseUrl: HttpUrl

    /** Raw path segments appended to [baseUrl]. Pass dynamic values as individual segments. */
    public val pathSegments: List<String> get() = emptyList()
    public val method: Method get() = Method.GET
    public val task: Task get() = Task.Plain
    public val headers: Map<String, String> get() = emptyMap()

    /** Default response body for stubs. */
    public val sampleData: ByteArray get() = byteArrayOf()
    public val validation: ValidationType get() = ValidationType.None
}

public enum class Method {
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE,
}

public enum class ValidationType {
    None,
    Successful,
}
