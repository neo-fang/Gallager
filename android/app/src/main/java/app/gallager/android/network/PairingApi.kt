package app.gallager.android.network

import app.gallager.android.model.PairingResult
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PairingApi(private val client: OkHttpClient) {
    suspend fun complete(
        serverUrl: String,
        pairingCode: String,
        deviceId: String,
        deviceName: String,
        publicKey: String,
        publicKeyId: String,
    ): PairingResult {
        val body = GallagerProtocol.pairingCompletion(
            pairingCode = pairingCode,
            deviceId = deviceId,
            deviceName = deviceName,
            publicKey = publicKey,
            publicKeyId = publicKeyId,
        )
        val request = Request.Builder()
            .url("${GallagerProtocol.normalizeHttpBase(serverUrl)}/api/pairing/complete")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val responseBody = client.await(request)
        return GallagerProtocol.parsePairingResponse(responseBody, serverUrl)
    }

    suspend fun unpair(serverUrl: String, pairId: String) {
        val request = Request.Builder()
            .url("${GallagerProtocol.normalizeHttpBase(serverUrl)}/api/pairing/$pairId")
            .delete()
            .build()
        client.await(request)
    }

    private suspend fun OkHttpClient.await(request: Request): String =
        suspendCancellableCoroutine { continuation ->
            val call = newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, error: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val body = it.body?.string().orEmpty()
                            if (!it.isSuccessful) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(IOException("Relay returned HTTP ${it.code}: $body"))
                                }
                            } else if (continuation.isActive) {
                                continuation.resume(body)
                            }
                        }
                    }
                },
            )
        }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
