package com.mid.varagh.core.data.remote

import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.network.model.ErrorDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs an API call and translates failures into [VaraghException]s the UI can explain:
 * no connection / timeouts -> Network, 401 -> Unauthorized, 404 -> NotFound,
 * 400/409/422 -> Rejected (with the server's message), anything else -> Server.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: VaraghException) {
    throw e
} catch (e: HttpException) {
    throw e.toVaraghException()
} catch (e: IOException) {
    throw VaraghException.Network(e)
} catch (e: kotlinx.serialization.SerializationException) {
    // The server answered with something we don't understand.
    throw VaraghException.Server(code = 0, cause = e)
}

private fun HttpException.toVaraghException(): VaraghException = when (val code = code()) {
    401 -> VaraghException.Unauthorized(this)
    404 -> VaraghException.NotFound("Resource")
    400, 409, 422 -> {
        val message = runCatching {
            response()?.errorBody()?.string()?.let { errorJson.decodeFromString(ErrorDto.serializer(), it).message }
        }.getOrNull()
        VaraghException.Rejected(code, message)
    }
    else -> VaraghException.Server(code, this)
}
