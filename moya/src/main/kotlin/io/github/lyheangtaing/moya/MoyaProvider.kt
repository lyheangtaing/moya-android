package io.github.lyheangtaing.moya

import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Executes targets with a reusable OkHttp client. Reuse the provider across requests. */
public class MoyaProvider<T : TargetType>(
    private val callFactory: Call.Factory = defaultClient,
    plugins: List<PluginType> = emptyList(),
    private val stubBehavior: (T) -> StubBehavior = { StubBehavior.Never },
    private val stubResponse: (T) -> StubResponse = { StubResponse(data = it.sampleData) },
) {
    private val plugins = plugins.toList()

    /**
     * Executes [target] and buffers its response on an OkHttp worker thread.
     * Cancelling the coroutine cancels the call. Status validation is opt-in per target.
     */
    public suspend fun request(target: T): MoyaResponse {
        val context = currentCoroutineContext()
        context.ensureActive()

        val result = try {
            val request = try {
                target.toRequest()
            } catch (failure: IllegalArgumentException) {
                throw MoyaException.RequestMapping(failure)
            }
            val prepared = plugins.fold(request) { current, plugin -> plugin.prepare(current, target) }
            val behavior = stubBehavior(target)
            plugins.forEach { it.willSend(prepared, target) }
            context.ensureActive()

            val response = when (behavior) {
                StubBehavior.Never -> execute(prepared)
                StubBehavior.Immediate -> stubResponse(target).toResponse(prepared)
                is StubBehavior.Delayed -> {
                    delay(behavior.milliseconds)
                    stubResponse(target).toResponse(prepared)
                }
            }
            if (target.validation == ValidationType.Successful) {
                response.filterSuccessfulStatusCodes()
            }
            Result.success(response)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Result.failure(failure)
        }

        context.ensureActive()
        plugins.forEach { it.didReceive(result, target) }
        return result.getOrThrow()
    }

    private suspend fun execute(request: Request): MoyaResponse = suspendCancellableCoroutine { continuation ->
        val call = callFactory.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        if (!continuation.isActive) return@suspendCancellableCoroutine

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(MoyaException.Transport(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val buffered = try {
                    response.use {
                        MoyaResponse(
                            statusCode = it.code,
                            headers = it.headers,
                            request = it.request,
                            data = it.body.bytes(),
                        )
                    }
                } catch (failure: IOException) {
                    continuation.resumeWithException(MoyaException.Transport(failure))
                    return
                } catch (failure: Exception) {
                    continuation.resumeWithException(failure)
                    return
                }
                continuation.resume(buffered)
            }
        })
    }

    private fun StubResponse.toResponse(request: Request): MoyaResponse =
        MoyaResponse(statusCode, headers, request, data.copyOf())

    private companion object {
        val defaultClient by lazy { OkHttpClient() }
    }
}
