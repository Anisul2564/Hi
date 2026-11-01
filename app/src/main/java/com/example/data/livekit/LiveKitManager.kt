package com.example.data.livekit

import android.content.Context
import com.example.model.LiveKitConnectionStatus
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.track.LocalAudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LiveKitManager(
    private val context: Context,
    private val tokenService: LiveKitTokenService = ProductionLiveKitTokenService()
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var eventsJob: Job? = null

    private var liveKitRoom: Room? = null
    private var localAudioTrack: LocalAudioTrack? = null

    private val _connectionStatus = MutableStateFlow(LiveKitConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<LiveKitConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isMicrophoneMuted = MutableStateFlow(true)
    val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isBackendTokenServiceReady: Boolean
        get() = tokenService.isBackendConfigured()

    /**
     * Initializes connection to LiveKit real-time WebRTC room.
     * Uses token issued by trusted backend.
     */
    suspend fun connectToRoom(
        liveKitServerUrl: String,
        token: String
    ): Result<Unit> {
        try {
            _connectionStatus.value = LiveKitConnectionStatus.CONNECTING
            _errorMessage.value = null

            val room = LiveKit.create(
                appContext = context.applicationContext,
                options = RoomOptions(adaptiveStream = true, dynacast = true)
            )
            liveKitRoom = room

            eventsJob?.cancel()
            eventsJob = scope.launch {
                room.events.events.collect { event ->
                    when (event) {
                        is RoomEvent.Connected -> {
                            _connectionStatus.value = LiveKitConnectionStatus.CONNECTED
                        }
                        is RoomEvent.Disconnected -> {
                            _connectionStatus.value = LiveKitConnectionStatus.DISCONNECTED
                        }
                        is RoomEvent.Reconnecting -> {
                            _connectionStatus.value = LiveKitConnectionStatus.RECONNECTING
                        }
                        is RoomEvent.Reconnected -> {
                            _connectionStatus.value = LiveKitConnectionStatus.CONNECTED
                        }
                        is RoomEvent.FailedToConnect -> {
                            _connectionStatus.value = LiveKitConnectionStatus.FAILED
                            _errorMessage.value = event.error.localizedMessage ?: "Failed to connect to LiveKit stage"
                        }
                        else -> Unit
                    }
                }
            }

            room.connect(
                url = liveKitServerUrl,
                token = token,
                options = ConnectOptions()
            )

            _connectionStatus.value = LiveKitConnectionStatus.CONNECTED
            return Result.success(Unit)
        } catch (e: Exception) {
            _connectionStatus.value = LiveKitConnectionStatus.FAILED
            _errorMessage.value = e.localizedMessage
            return Result.failure(e)
        }
    }

    /**
     * Enables or disables microphone publishing on the active LiveKit room.
     * Only invoked when user holds an active speaker seat.
     */
    suspend fun setMicrophoneEnabled(enabled: Boolean): Result<Unit> {
        val room = liveKitRoom ?: return Result.failure(IllegalStateException("Not connected to room"))
        return try {
            room.localParticipant.setMicrophoneEnabled(enabled)
            _isMicrophoneMuted.value = !enabled
            Result.success(Unit)
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage
            Result.failure(e)
        }
    }

    /**
     * Disconnects from the LiveKit room and releases WebRTC audio resources.
     */
    fun disconnect() {
        eventsJob?.cancel()
        eventsJob = null
        try {
            liveKitRoom?.disconnect()
            liveKitRoom?.release()
        } catch (_: Exception) {
        } finally {
            liveKitRoom = null
            localAudioTrack = null
            _connectionStatus.value = LiveKitConnectionStatus.DISCONNECTED
            _isMicrophoneMuted.value = true
        }
    }
}
