package com.example.data.livekit

import com.example.model.LiveKitTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface LiveKitTokenService {
    /**
     * Requests a cryptographically signed, short-lived LiveKit access token from the trusted backend.
     * The backend derives the user identity from secure Firebase Authentication (Firebase ID Token),
     * preventing any client from spoofing or choosing arbitrary privileged identities.
     *
     * @param roomId Target room id to connect to
     * @param firebaseIdToken Verified Firebase ID token of the authenticated caller
     * @return Result containing LiveKitTokenResponse if successful
     */
    suspend fun fetchLiveKitToken(roomId: String, firebaseIdToken: String): Result<LiveKitTokenResponse>

    /**
     * Indicates whether the live backend token service endpoint is configured.
     * In accordance with security requirements, client-side secret generation is prohibited.
     */
    fun isBackendConfigured(): Boolean
}

/**
 * Production implementation for LiveKit token exchange.
 * Calls the trusted Firebase Cloud Functions endpoint:
 * POST $backendBaseUrl/livekit/token
 * Authorization: Bearer <firebaseIdToken>
 * Body: { "roomId": roomId }
 *
 * If backend token endpoint is not configured or awaiting deployment, it cleanly reports that backend token generation
 * remains pending rather than faking tokens or compromising secrets.
 */
class ProductionLiveKitTokenService(
    private val backendBaseUrl: String? = null,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) : LiveKitTokenService {

    override fun isBackendConfigured(): Boolean {
        return !backendBaseUrl.isNullOrBlank()
    }

    override suspend fun fetchLiveKitToken(roomId: String, firebaseIdToken: String): Result<LiveKitTokenResponse> = withContext(Dispatchers.IO) {
        val url = backendBaseUrl
        if (url.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "LiveKit credentials pending deployment/configuration. " +
                    "To prevent token spoofing and credential leaks, LiveKit access tokens must be minted " +
                    "by the trusted Firebase backend with LIVEKIT_API_KEY and LIVEKIT_API_SECRET."
                )
            )
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("roomId", roomId)
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val endpointUrl = if (url.endsWith("/")) "${url}livekit/token" else "$url/livekit/token"

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $firebaseIdToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    JSONObject(responseBody).optString("message", "HTTP error ${response.code}")
                } catch (_: Exception) {
                    "HTTP error ${response.code}: $responseBody"
                }
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            val json = JSONObject(responseBody)
            val token = json.getString("token")
            val roomName = json.optString("roomName", roomId)
            val identity = json.getString("identity")
            val expiresAt = json.optLong("expiresAt", System.currentTimeMillis() + 3600000L)
            val serverUrl = json.optString("serverUrl", null)
            val roomRole = json.optString("roomRole", "LISTENER")
            val canPublish = json.optBoolean("canPublish", false)

            Result.success(
                LiveKitTokenResponse(
                    token = token,
                    roomName = roomName,
                    identity = identity,
                    expiresAt = expiresAt,
                    serverUrl = serverUrl,
                    roomRole = roomRole,
                    canPublish = canPublish
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

