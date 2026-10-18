package dev.profilebridge.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.profilebridge.app.BuildConfig
import dev.profilebridge.app.platform.AndroidTlsIdentity
import dev.profilebridge.core.tls.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProbeStatus { IDLE, STARTING, LISTENING, FINISHED, CHECKING, REACHABLE, UNAVAILABLE, TLS_REJECTED, INVALID, SELF, NOT_PERMITTED }
data class ProbeState(
    val sessionCode: String? = null,
    val input: String = "",
    val listener: ProbeStatus = ProbeStatus.IDLE,
    val result: ProbeStatus = ProbeStatus.IDLE,
    val remainingSeconds: Long = 0,
    val clientEvent: DiagnosticEvent? = null,
    val serverEvent: DiagnosticEvent? = null,
    val serverFailure: DiagnosticEvent? = null,
    val selfChecking: Boolean = false,
    val report: String? = null,
) {
    override fun toString(): String = "ProbeState([REDACTED])"
}

class ProbeViewModel : ViewModel() {
    private val probe = TlsProbe()
    private val journal = DiagnosticJournal()
    private val mutable = MutableStateFlow(ProbeState())
    val state = mutable.asStateFlow()
    private var session: TlsProbeSession? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null

    private fun attempt(side: DiagnosticSide, server: Boolean = false) = DiagnosticAttempt(side) { event ->
        journal.add(event)
        mutable.update {
            if (server) it.copy(serverEvent = event,
                serverFailure = if (event.status == DiagnosticStatus.FAILURE) event else it.serverFailure)
            else it.copy(clientEvent = event)
        }
    }

    fun edit(value: String) {
        if (clientJob?.isActive != true && value.length <= PairingCode.MAX_LENGTH) {
            mutable.update { it.copy(input = value, result = ProbeStatus.IDLE) }
        }
    }

    fun start() {
        if (serverJob?.isActive == true || state.value.selfChecking) return
        mutable.update { it.copy(listener = ProbeStatus.STARTING, serverFailure = null) }
        serverJob = viewModelScope.launch {
            val diagnostic = attempt(DiagnosticSide.SERVER, server = true)
            var opened: TlsProbeSession? = null
            var timer: Job? = null
            try {
                val listener = probe.listen(AndroidTlsIdentity.load(diagnostic), diagnostic)
                opened = listener
                session = listener
                mutable.update { it.copy(sessionCode = listener.code.encodeForManualTransfer(), listener = ProbeStatus.LISTENING) }
                timer = launch {
                    while (true) {
                        mutable.update { it.copy(remainingSeconds = listener.remainingSeconds) }
                        delay(250)
                    }
                }
                listener.serve()
            } catch (cancelled: CancellationException) { diagnostic.failure(cancelled); throw cancelled }
            catch (error: Exception) { diagnostic.failure(error) }
            finally {
                timer?.cancel()
                opened?.close()
                session = null
                mutable.update { it.copy(sessionCode = null, listener = ProbeStatus.FINISHED, remainingSeconds = 0) }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        session?.close()
    }

    fun ping() {
        if (clientJob?.isActive == true) return
        val input = state.value.input
        val code = PairingCode.parse(input)
        if (code == null || input == state.value.sessionCode) {
            code?.destroy()
            mutable.update { it.copy(result = if (code == null) ProbeStatus.INVALID else ProbeStatus.SELF) }
            return
        }
        mutable.update { it.copy(input = "", result = ProbeStatus.CHECKING) }
        clientJob = viewModelScope.launch {
            try {
                val result = probe.ping(code, attempt(DiagnosticSide.CLIENT))
                mutable.update { it.copy(result = status(result)) }
            } finally {
                code.destroy()
                mutable.update { if (it.result == ProbeStatus.CHECKING) it.copy(result = ProbeStatus.FINISHED) else it }
            }
        }
    }

    fun selfCheck() {
        if (clientJob?.isActive == true || serverJob?.isActive == true) return
        mutable.update { it.copy(selfChecking = true, result = ProbeStatus.CHECKING, serverFailure = null) }
        clientJob = viewModelScope.launch {
            val client = attempt(DiagnosticSide.SELF_CHECK)
            val server = attempt(DiagnosticSide.SERVER, server = true)
            try {
                val result = probe.selfCheck(AndroidTlsIdentity.load(server), client, server)
                mutable.update { it.copy(result = status(result)) }
            } catch (cancelled: CancellationException) { client.failure(cancelled); throw cancelled }
            catch (error: Exception) { server.failure(error); mutable.update { it.copy(result = ProbeStatus.TLS_REJECTED) } }
            finally { mutable.update { it.copy(selfChecking = false,
                result = if (it.result == ProbeStatus.CHECKING) ProbeStatus.FINISHED else it.result) } }
        }
    }

    fun previewReport() { mutable.update { it.copy(report = "Version: ${BuildConfig.VERSION_NAME}\n" + journal.report()) } }
    fun dismissReport() { mutable.update { it.copy(report = null) } }

    /** Self-check is screen-bound. A manual listener remains best-effort until stop/TTL. */
    fun screenStopped() {
        if (state.value.selfChecking) clientJob?.cancel()
        mutable.update { it.copy(input = "") }
    }

    private fun status(result: ProbeResult) = when (result) {
        ProbeResult.REACHABLE -> ProbeStatus.REACHABLE
        ProbeResult.TLS_REJECTED -> ProbeStatus.TLS_REJECTED
        ProbeResult.UNAVAILABLE -> ProbeStatus.UNAVAILABLE
        ProbeResult.NOT_PERMITTED -> ProbeStatus.NOT_PERMITTED
    }
    override fun onCleared() { session?.close() }
}
