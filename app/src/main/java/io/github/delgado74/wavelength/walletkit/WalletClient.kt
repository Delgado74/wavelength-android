package io.github.delgado74.wavelength.walletkit

import android.util.Base64
import engineering.lightning.wavewalletdk.mobile.Mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Thrown when an embedded-wallet call fails. */
class WalletException(message: String?) : Exception(message)

/**
 * Idiomatic Kotlin facade over the gomobile `Mobile` bindings.
 *
 * Every call runs the blocking binding on [Dispatchers.IO]. One embedded daemon
 * runs per process, so a single instance is enough for the whole app.
 */
class WalletClient(private val json: Json = DEFAULT_JSON) {

    val isRunning: Boolean get() = Mobile.isRunning()

    suspend fun start(config: WalletConfig) = io {
        Mobile.start(json.encodeToString(WalletConfig.serializer(), config))
    }

    suspend fun stop() = io { Mobile.stop() }

    suspend fun getInfo(): Info = decode(io { Mobile.getInfo() })

    suspend fun status(): Status = decode(io { Mobile.status() })

    suspend fun balance(): Balance = decode(io { Mobile.balance() })

    suspend fun createWallet(
        walletPassword: ByteArray,
        mnemonic: List<String> = emptyList(),
        recoverState: Boolean = false,
        recoveryWindow: Int = 0,
    ): CreateWalletResult {
        val request = CreateWalletRequest(
            mnemonic = mnemonic,
            walletPassword = base64(walletPassword),
            recoverState = recoverState,
            recoveryWindow = recoveryWindow,
        )
        return decode(io { Mobile.createWallet(encode(CreateWalletRequest.serializer(), request)) })
    }

    suspend fun unlockWallet(walletPassword: ByteArray): UnlockWalletResult {
        val request = UnlockWalletRequest(base64(walletPassword))
        return decode(io { Mobile.unlockWallet(encode(UnlockWalletRequest.serializer(), request)) })
    }

    suspend fun receiveLightning(amountSat: Long, memo: String = ""): ReceiveResult {
        val request = ReceiveRequest(amountSat = amountSat, memo = memo)
        return decode(io { Mobile.receive(encode(ReceiveRequest.serializer(), request)) })
    }

    suspend fun newDepositAddress(amountSatHint: Long = 0): DepositResult {
        val request = DepositRequest(amountSatHint = amountSatHint)
        return decode(io { Mobile.deposit(encode(DepositRequest.serializer(), request)) })
    }

    suspend fun prepareSend(
        invoice: String? = null,
        onchainAddress: String? = null,
        amountSat: Long = 0,
        note: String = "",
        maxFeeSat: Long = 0,
        sweepAll: Boolean = false,
    ): PrepareSendResult {
        val request = PrepareSendRequest(
            invoice = invoice,
            onchainAddress = onchainAddress,
            amountSat = amountSat,
            note = note,
            maxFeeSat = maxFeeSat,
            sweepAll = sweepAll,
        )
        return decode(io { Mobile.prepareSend(encode(PrepareSendRequest.serializer(), request)) })
    }

    suspend fun send(sendIntentId: String): SendResult {
        val request = SendPreparedRequest(sendIntentId)
        return decode(io { Mobile.sendPrepared(encode(SendPreparedRequest.serializer(), request)) })
    }

    suspend fun listActivity(limit: Long = 50, cursor: String = ""): ListResult {
        val request = ListRequest(limit = limit, cursor = cursor)
        return decode(io { Mobile.list(encode(ListRequest.serializer(), request)) })
    }

    suspend fun exit(outpoint: String, destination: String = ""): ExitResult {
        val request = ExitRequest(outpoint, destination)
        return decode(io { Mobile.exit(encode(ExitRequest.serializer(), request)) })
    }

    /**
     * Stream wallet activity. The flow completes on a clean end-of-stream and
     * fails with [WalletException] on a real error. Cancelling the collection
     * closes the underlying pull subscription.
     */
    fun activity(includeExisting: Boolean = true): Flow<Entry> = channelFlow {
        val subscription = Mobile.subscribe(
            encode(SubscribeRequest.serializer(), SubscribeRequest(includeExisting)),
        )
        val pump = launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val next = try {
                        subscription.next()
                    } catch (e: Exception) {
                        if (isEndOfStream(e)) break else throw WalletException(e.message)
                    }
                    trySend(json.decodeFromString(Entry.serializer(), String(next)))
                }
                close()
            } catch (t: Throwable) {
                close(t)
            }
        }
        awaitClose {
            subscription.close()
            pump.cancel()
        }
    }

    private suspend inline fun <T> io(crossinline block: () -> T): T =
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: WalletException) {
            throw e
        } catch (e: Exception) {
            throw WalletException(e.message)
        }

    private inline fun <reified T> decode(bytes: ByteArray): T =
        json.decodeFromString(String(bytes))

    private fun <T> encode(serializer: kotlinx.serialization.SerializationStrategy<T>, value: T): ByteArray =
        json.encodeToString(serializer, value).toByteArray()

    companion object {
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
        }

        private fun base64(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.NO_WRAP)

        private fun isEndOfStream(e: Exception): Boolean =
            e.message?.contains("EOF", ignoreCase = true) == true
    }
}

@Serializable
private data class CreateWalletRequest(
    @SerialName("Mnemonic") val mnemonic: List<String> = emptyList(),
    @SerialName("WalletPassword") val walletPassword: String,
    @SerialName("RecoverState") val recoverState: Boolean = false,
    @SerialName("RecoveryWindow") val recoveryWindow: Int = 0,
)

@Serializable
private data class UnlockWalletRequest(
    @SerialName("WalletPassword") val walletPassword: String,
)

@Serializable
private data class ReceiveRequest(
    @SerialName("AmountSat") val amountSat: Long,
    @SerialName("Memo") val memo: String = "",
)

@Serializable
private data class DepositRequest(
    @SerialName("AmountSatHint") val amountSatHint: Long = 0,
)

@Serializable
private data class PrepareSendRequest(
    @SerialName("Invoice") val invoice: String? = null,
    @SerialName("OnchainAddress") val onchainAddress: String? = null,
    @SerialName("AmountSat") val amountSat: Long = 0,
    @SerialName("Note") val note: String = "",
    @SerialName("MaxFeeSat") val maxFeeSat: Long = 0,
    @SerialName("SweepAll") val sweepAll: Boolean = false,
)

@Serializable
private data class SendPreparedRequest(
    @SerialName("SendIntentID") val sendIntentId: String,
)

@Serializable
private data class ListRequest(
    @SerialName("View") val view: String = "activity",
    @SerialName("Limit") val limit: Long = 50,
    @SerialName("Cursor") val cursor: String = "",
)

@Serializable
private data class SubscribeRequest(
    @SerialName("IncludeExisting") val includeExisting: Boolean,
)

@Serializable
private data class ExitRequest(
    @SerialName("Outpoint") val outpoint: String,
    @SerialName("Destination") val destination: String = "",
)
