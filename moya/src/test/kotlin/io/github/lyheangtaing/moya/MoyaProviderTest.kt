package io.github.lyheangtaing.moya

import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MoyaProviderTest {
    @Test
    fun `request preserves base path and encodes path segments and repeated query values`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse(body = """{"ok":true}"""))
            val target = Endpoint(
                baseUrl = server.url("/api/v1/"),
                pathSegments = listOf("users", "a/b %"),
                task = Task.Query(listOf("tag" to "a+b", "tag" to "x & y")),
            )

            val response = MoyaProvider<Endpoint>().request(target)

            assertEquals("""{"ok":true}""", response.bodyAsString())
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals(
                "/api/v1/users/a%2Fb%20%25?tag=a%2Bb&tag=x%20%26%20y",
                request.target,
            )
        }
    }

    @Test
    fun `JSON sends UTF-8 with its media type and query`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse(code = 201))
            val target = Endpoint(
                baseUrl = server.url("/"),
                pathSegments = listOf("users"),
                method = Method.POST,
                task = Task.json("""{"name":"សុខ"}""", listOf("source" to "mobile")),
            )

            val response = MoyaProvider<Endpoint>().request(target)

            assertEquals(201, response.statusCode)
            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("/users?source=mobile", request.target)
            assertEquals("application/json; charset=utf-8", request.headers["Content-Type"])
            assertEquals("""{"name":"សុខ"}""", request.body!!.utf8())
        }
    }

    @Test
    fun `form task sends encoded key and value pairs`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse())
            val target = Endpoint(
                baseUrl = server.url("/"),
                method = Method.POST,
                task = Task.Form(listOf("q" to "a+b &c", "flag" to "")),
            )

            MoyaProvider<Endpoint>().request(target)

            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("application/x-www-form-urlencoded", request.headers["Content-Type"])
            assertEquals("q=a%2Bb+%26c&flag=", request.body!!.utf8())
        }
    }

    @Test
    fun `plain POST sends an empty body`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse())
            MoyaProvider<Endpoint>().request(Endpoint(server.url("/"), method = Method.POST))

            val request = server.takeRequest(2, TimeUnit.SECONDS)!!
            assertEquals("POST", request.method)
            assertEquals("0", request.headers["Content-Length"])
        }
    }

    @Test
    fun `dot segments are rejected before sending`() = runBlocking {
        val target = Endpoint(unusedUrl, pathSegments = listOf("users", ".."))
        val provider = stubProvider()

        val failure = runCatching { provider.request(target) }.exceptionOrNull()
        assertTrue(failure is MoyaException.RequestMapping)
    }

    @Test
    fun `GET rejects a request body before sending`() = runBlocking {
        val target = Endpoint(unusedUrl, task = Task.json("{}"))

        val failure = runCatching { stubProvider().request(target) }.exceptionOrNull()
        assertTrue(failure is MoyaException.RequestMapping)
    }

    @Test
    fun `HTTP status is available by default and validation retains the response`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            repeat(2) { server.enqueue(MockResponse(code = 404, body = "missing")) }
            val target = Endpoint(server.url("/"))
            val provider = MoyaProvider<Endpoint>()

            val response = provider.request(target)
            assertFalse(response.isSuccessful)
            assertEquals(404, response.statusCode)

            val failure = runCatching {
                provider.request(target.copy(validation = ValidationType.Successful))
            }.exceptionOrNull() as MoyaException.HttpStatus
            assertEquals(404, failure.response.statusCode)
            assertEquals("missing", failure.response.bodyAsString())
        }
    }

    @Test
    fun `plugins prepare the outgoing request and observe its result`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse(body = "ok"))
            val events = mutableListOf<String>()
            var received: MoyaResponse? = null
            val plugin = object : PluginType {
                override fun prepare(request: Request, target: TargetType): Request {
                    events += "prepare"
                    return request.newBuilder().header("X-Trace", "plugin").build()
                }

                override fun willSend(request: Request, target: TargetType) {
                    events += "send:${request.header("X-Trace")}"
                }

                override fun didReceive(result: Result<MoyaResponse>, target: TargetType) {
                    events += "receive"
                    received = result.getOrThrow()
                }
            }
            val provider = MoyaProvider<Endpoint>(plugins = listOf(plugin))

            val response = provider.request(Endpoint(server.url("/"), headers = mapOf("X-Trace" to "target")))

            assertEquals("plugin", server.takeRequest(2, TimeUnit.SECONDS)!!.headers["X-Trace"])
            assertEquals(listOf("prepare", "send:plugin", "receive"), events)
            assertSame(response, received)
        }
    }

    @Test
    fun `immediate stub uses sample bytes without creating an OkHttp call`() = runBlocking {
        val response = stubProvider().request(Endpoint(unusedUrl, sampleData = "fixture".encodeToByteArray()))

        assertEquals(200, response.statusCode)
        assertEquals("fixture", response.bodyAsString())
    }

    @Test
    fun `custom stub preserves headers and body through status validation`() = runBlocking {
        val provider = MoyaProvider<Endpoint>(
            callFactory = failOnNetwork,
            stubBehavior = { StubBehavior.Immediate },
            stubResponse = {
                StubResponse(
                    statusCode = 429,
                    headers = headersOf("Content-Type", "text/plain; charset=iso-8859-1"),
                    data = byteArrayOf(0xe9.toByte()),
                )
            },
        )

        val failure = runCatching {
            provider.request(Endpoint(unusedUrl, validation = ValidationType.Successful))
        }.exceptionOrNull() as MoyaException.HttpStatus

        assertEquals(429, failure.response.statusCode)
        assertEquals("é", failure.response.bodyAsString())
    }

    @Test
    fun `delayed stub honors cancellation`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val provider = MoyaProvider<Endpoint>(
            callFactory = failOnNetwork,
            stubBehavior = { StubBehavior.Delayed(60_000) },
            plugins = listOf(object : PluginType {
                override fun willSend(request: Request, target: TargetType) {
                    started.complete(Unit)
                }

                override fun didReceive(result: Result<MoyaResponse>, target: TargetType) {
                    error("A cancelled stub must not deliver a result")
                }
            }),
        )

        val request = launch { provider.request(Endpoint(unusedUrl)) }
        withTimeout(5_000) { started.await() }
        withTimeout(5_000) { request.cancelAndJoin() }
        assertTrue(request.isCancelled)
    }

    @Test
    fun `network cancellation cancels the underlying OkHttp call`() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().headersDelay(5, TimeUnit.SECONDS).build())
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val client = OkHttpClient.Builder().eventListener(object : EventListener() {
                override fun callStart(call: Call) {
                    started.complete(Unit)
                }

                override fun canceled(call: Call) {
                    cancelled.complete(Unit)
                }
            }).build()
            val provider = MoyaProvider<Endpoint>(callFactory = client)
            val request = launch { provider.request(Endpoint(server.url("/"))) }

            withTimeout(5_000) { started.await() }
            withTimeout(5_000) { request.cancelAndJoin() }
            withTimeout(5_000) { cancelled.await() }
        }
    }

    @Test
    fun `transport failures use a distinct exception and notify plugins`() = runBlocking {
        val client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .dns { throw java.net.UnknownHostException("test DNS failure") }
            .build()
        var observed: Throwable? = null
        val provider = MoyaProvider<Endpoint>(
            callFactory = client,
            plugins = listOf(object : PluginType {
                override fun didReceive(result: Result<MoyaResponse>, target: TargetType) {
                    observed = result.exceptionOrNull()
                }
            }),
        )

        val failure = runCatching { provider.request(Endpoint(unusedUrl)) }.exceptionOrNull()

        assertTrue(failure is MoyaException.Transport)
        assertTrue(failure?.cause is IOException)
        assertSame(failure, observed)
    }

    private fun stubProvider() = MoyaProvider<Endpoint>(
        callFactory = failOnNetwork,
        stubBehavior = { StubBehavior.Immediate },
    )

    private data class Endpoint(
        override val baseUrl: HttpUrl,
        override val pathSegments: List<String> = emptyList(),
        override val method: Method = Method.GET,
        override val task: Task = Task.Plain,
        override val headers: Map<String, String> = emptyMap(),
        override val sampleData: ByteArray = byteArrayOf(),
        override val validation: ValidationType = ValidationType.None,
    ) : TargetType

    private companion object {
        val unusedUrl = HttpUrl.Builder().scheme("https").host("example.invalid").build()
        val failOnNetwork = Call.Factory { error("A stub must not create a network call") }
    }
}
