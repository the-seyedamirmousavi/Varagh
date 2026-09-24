package com.mid.varagh.core.domain

/** Errors the UI knows how to explain. Repositories translate lower-level failures into these. */
sealed class VaraghException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** No connection / server unreachable / timeout. */
    class Network(cause: Throwable? = null) : VaraghException("Network unavailable", cause)

    /** Credentials rejected or session expired. */
    class Unauthorized(cause: Throwable? = null) : VaraghException("Unauthorized", cause)

    /** Server returned an unexpected error. */
    class Server(val code: Int, cause: Throwable? = null) : VaraghException("Server error $code", cause)

    class NotFound(what: String) : VaraghException("$what not found")

    /** Called a server-only feature while USE_REMOTE_BACKEND is false. */
    class FeatureUnavailable(feature: String) : VaraghException("$feature requires the remote backend")
}
