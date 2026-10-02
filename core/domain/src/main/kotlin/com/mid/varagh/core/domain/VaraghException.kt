package com.mid.varagh.core.domain

/** Errors the UI knows how to explain. Repositories translate lower-level failures into these. */
sealed class VaraghException(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    /** No connection / server unreachable / timeout. */
    class Network(cause: Throwable? = null) : VaraghException("Network unavailable", cause)

    /** Credentials rejected or session expired. */
    class Unauthorized(cause: Throwable? = null) : VaraghException("Unauthorized", cause)

    /** Server returned an unexpected error. */
    class Server(val code: Int, cause: Throwable? = null) : VaraghException("Server error $code", cause)

    /** Request rejected by the server, e.g. username taken (HTTP 409/422). */
    class Rejected(val code: Int, val serverMessage: String?) : VaraghException(serverMessage ?: "Rejected ($code)")

    class NotFound(what: String) : VaraghException("$what not found")

    /** Called a server-only feature while USE_REMOTE_BACKEND is false. */
    class FeatureUnavailable(feature: String) : VaraghException("$feature requires the remote backend")

    /** The picked file is not a readable PDF (corrupt, encrypted or not a PDF). */
    class InvalidFile(cause: Throwable? = null) : VaraghException("Not a readable PDF", cause)

    /** The book's file was moved/deleted or the app lost permission to read it. */
    class FileUnavailable(cause: Throwable? = null) : VaraghException("File unavailable", cause)

    /** A relinked file is a different document than the one in the library. */
    class DifferentFile : VaraghException("This is a different file")

    /** A backup file could not be read (wrong format or newer version). */
    class InvalidBackup(cause: Throwable? = null) : VaraghException("Invalid backup", cause)
}
