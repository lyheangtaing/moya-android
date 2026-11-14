package io.github.lyheangtaing.moya

import okhttp3.Request

/**
 * Hooks run in the requesting coroutine, in registration order. Keep them fast.
 * A plugin shared by concurrent requests must protect its mutable state.
 */
public interface PluginType {
    public fun prepare(request: Request, target: TargetType): Request = request
    public fun willSend(request: Request, target: TargetType) {}
    public fun didReceive(result: Result<MoyaResponse>, target: TargetType) {}
}
