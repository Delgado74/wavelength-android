package io.github.delgado74.wavelength

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.delgado74.wavelength.walletkit.Balance
import io.github.delgado74.wavelength.walletkit.Entry
import io.github.delgado74.wavelength.walletkit.Info
import io.github.delgado74.wavelength.walletkit.PrepareSendResult
import io.github.delgado74.wavelength.walletkit.SendResult
import io.github.delgado74.wavelength.walletkit.WalletClient
import io.github.delgado74.wavelength.walletkit.WalletConfig
import io.github.delgado74.wavelength.walletkit.WalletState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class Phase {
    STARTING,
    ONBOARDING,
    BACKUP,
    UNLOCK,
    READY,
    ERROR,
}

data class UiState(
    val phase: Phase = Phase.STARTING,
    val network: String = WalletConfig.SIGNET,
    val busy: Boolean = false,
    val error: String? = null,
    val info: Info? = null,
    val balance: Balance = Balance(),
    val pendingCount: Long = 0,
    val activity: List<Entry> = emptyList(),
    val mnemonic: List<String> = emptyList(),
    val invoice: String? = null,
    val depositAddress: String? = null,
    val quote: PrepareSendResult? = null,
    val lastSend: SendResult? = null,
    val message: String? = null,
)

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = WalletClient()
    private val _state = MutableStateFlow(
        UiState(network = prefs.getString(KEY_NETWORK, WalletConfig.SIGNET) ?: WalletConfig.SIGNET),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var pollJob: Job? = null

    init {
        boot()
    }

    fun boot() {
        viewModelScope.launch {
            _state.update { it.copy(phase = Phase.STARTING, error = null, busy = true) }
            try {
                openClient()
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        phase = Phase.ERROR,
                        busy = false,
                        error = t.message ?: t.toString(),
                    )
                }
            }
        }
    }

    fun retry() = boot()

    fun selectNetwork(network: String) {
        if (network == _state.value.network) return
        viewModelScope.launch {
            streamJob?.cancel()
            pollJob?.cancel()
            _state.update { it.copy(busy = true, error = null, phase = Phase.STARTING) }
            try {
                if (client.isRunning) client.stop()
                prefs.edit().putString(KEY_NETWORK, network).apply()
                _state.update { it.copy(network = network) }
                openClient()
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        phase = Phase.ERROR,
                        busy = false,
                        error = t.message ?: t.toString(),
                    )
                }
            }
        }
    }

    fun createWallet(password: String) = launchAction {
        val result = client.createWallet(password.encodeToByteArray())
        if (result.mnemonic.isEmpty()) {
            _state.update { it.copy(info = client.getInfo(), phase = Phase.READY) }
            startSync()
        } else {
            _state.update {
                it.copy(
                    info = client.getInfo(),
                    phase = Phase.BACKUP,
                    mnemonic = result.mnemonic,
                )
            }
        }
    }

    fun confirmBackup() {
        _state.update {
            it.copy(
                phase = Phase.READY,
                mnemonic = emptyList(),
                message = "Wallet creada. Guarda tu frase semilla.",
            )
        }
        startSync()
    }

    fun restoreWallet(words: List<String>, password: String) = launchAction {
        client.createWallet(
            walletPassword = password.encodeToByteArray(),
            mnemonic = words,
            recoverState = true,
        )
        _state.update {
            it.copy(
                info = client.getInfo(),
                phase = Phase.READY,
                message = "Wallet restaurada. Sincronizando fondos.",
            )
        }
        startSync()
    }

    fun unlockWallet(password: String) = launchAction {
        if (!client.isRunning) client.start(config())
        client.unlockWallet(password.encodeToByteArray())
        _state.update {
            it.copy(info = client.getInfo(), phase = Phase.READY)
        }
        startSync()
    }

    fun lock() {
        viewModelScope.launch {
            streamJob?.cancel()
            pollJob?.cancel()
            try {
                client.stop()
            } catch (_: Throwable) {
            }
            _state.update {
                it.copy(
                    phase = Phase.UNLOCK,
                    info = null,
                    balance = Balance(),
                    pendingCount = 0,
                    activity = emptyList(),
                    quote = null,
                    invoice = null,
                    depositAddress = null,
                    lastSend = null,
                )
            }
        }
    }

    fun refresh() = refresh(showBusy = true)

    fun createInvoice(amountSat: Long, memo: String) = launchAction {
        val result = client.receiveLightning(amountSat, memo)
        _state.update { it.copy(invoice = result.invoice, depositAddress = null) }
    }

    fun createDeposit(amountHintSat: Long) = launchAction {
        val result = client.newDepositAddress(amountHintSat)
        _state.update { it.copy(depositAddress = result.address, invoice = null) }
    }

    fun prepareSend(
        invoice: String?,
        onchainAddress: String?,
        amountSat: Long,
        note: String,
        maxFeeSat: Long,
    ) = launchAction {
        val quote = client.prepareSend(
            invoice = invoice,
            onchainAddress = onchainAddress,
            amountSat = amountSat,
            note = note,
            maxFeeSat = maxFeeSat,
        )
        _state.update { it.copy(quote = quote, lastSend = null) }
    }

    fun confirmSend() {
        val quote = _state.value.quote ?: return
        launchAction {
            val result = client.send(quote.sendIntentId)
            _state.update {
                it.copy(
                    quote = null,
                    lastSend = result,
                    message = "Pago enviado",
                )
            }
            refresh(showBusy = false)
        }
    }

    fun clearQuote() = _state.update { it.copy(quote = null) }

    fun clearLastSend() = _state.update { it.copy(lastSend = null) }

    fun clearGenerated() = _state.update { it.copy(invoice = null, depositAddress = null) }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun clearError() = _state.update { it.copy(error = null) }

    private suspend fun openClient() {
        if (client.isRunning) client.stop()
        client.start(config())
        val info = client.getInfo()
        val phase = when (info.walletState) {
            WalletState.NONE -> Phase.ONBOARDING
            WalletState.LOCKED -> Phase.UNLOCK
            else -> Phase.READY
        }
        _state.update {
            it.copy(
                info = info,
                phase = phase,
                busy = false,
                network = info.network.ifBlank { it.network },
            )
        }
        if (phase == Phase.READY) startSync()
    }

    private fun startSync() {
        streamJob?.cancel()
        pollJob?.cancel()
        refresh(showBusy = false)
        streamJob = viewModelScope.launch {
            try {
                client.activity(includeExisting = true).collect { entry ->
                    _state.update { current ->
                        val index = current.activity.indexOfFirst { it.id == entry.id }
                        val updated = if (index >= 0) {
                            current.activity.toMutableList().also { it[index] = entry }
                        } else {
                            listOf(entry) + current.activity
                        }
                        current.copy(activity = updated.take(MAX_ACTIVITY))
                    }
                }
            } catch (_: Throwable) {
            }
        }
        pollJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                refresh(showBusy = false)
            }
        }
    }

    private fun refresh(showBusy: Boolean) {
        if (_state.value.phase != Phase.READY) return
        viewModelScope.launch {
            if (showBusy) {
                _state.update { it.copy(busy = true, error = null) }
            }
            try {
                val status = client.status()
                _state.update {
                    it.copy(balance = status.balance, pendingCount = status.pendingCount)
                }
                val page = client.listActivity(limit = 50)
                page.activity?.let { activity ->
                    _state.update { it.copy(activity = activity.entries) }
                }
            } catch (t: Throwable) {
                if (showBusy) {
                    _state.update { it.copy(error = t.message ?: t.toString()) }
                }
            } finally {
                if (showBusy) {
                    _state.update { it.copy(busy = false) }
                }
            }
        }
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                block()
            } catch (t: Throwable) {
                _state.update { it.copy(error = t.message ?: t.toString()) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun config(): WalletConfig =
        WalletConfig.forNetwork(_state.value.network, dataDir(_state.value.network))

    private fun dataDir(network: String): String =
        File(getApplication<Application>().filesDir, "wavelength/$network").absolutePath

    companion object {
        private const val PREFS = "wavelength"
        private const val KEY_NETWORK = "network"
        private const val MAX_ACTIVITY = 200
    }
}
