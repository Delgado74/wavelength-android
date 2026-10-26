package io.github.delgado74.wavelength.walletkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON models mirroring the `wavewalletdk` Go DTOs.
 *
 * The Go structs do not carry json tags, so the wire keys are the Go field
 * names in PascalCase. Keep the @SerialName values exactly as they appear here.
 */

enum class WalletState(val code: Int) {
    UNSPECIFIED(0),
    NONE(1),
    LOCKED(2),
    READY(3),
    SYNCING(4);

    companion object {
        fun fromCode(code: Int): WalletState =
            entries.firstOrNull { it.code == code } ?: UNSPECIFIED
    }
}

@Serializable
data class ServerInfo(
    @SerialName("FreeRefreshWindowBlocks") val freeRefreshWindowBlocks: Long = 0,
)

@Serializable
data class Info(
    @SerialName("Version") val version: String = "",
    @SerialName("Commit") val commit: String = "",
    @SerialName("Network") val network: String = "",
    @SerialName("BlockHeight") val blockHeight: Long = 0,
    @SerialName("ServerConnected") val serverConnected: Boolean = false,
    @SerialName("WalletType") val walletType: String = "",
    @SerialName("WalletState") val walletStateCode: Int = 0,
    @SerialName("IdentityPubKey") val identityPubKey: String = "",
    @SerialName("ServerInfo") val serverInfo: ServerInfo? = null,
) {
    val walletState: WalletState get() = WalletState.fromCode(walletStateCode)
    val walletReady: Boolean get() = walletState == WalletState.READY
}

@Serializable
data class Balance(
    @SerialName("ConfirmedSat") val confirmedSat: Long = 0,
    @SerialName("PendingInSat") val pendingInSat: Long = 0,
    @SerialName("PendingOutSat") val pendingOutSat: Long = 0,
    @SerialName("CreditAvailableSat") val creditAvailableSat: Long = 0,
    @SerialName("CreditReservedSat") val creditReservedSat: Long = 0,
)

@Serializable
data class Status(
    @SerialName("Ready") val ready: Boolean = false,
    @SerialName("Unlocked") val unlocked: Boolean = false,
    @SerialName("Network") val network: String = "",
    @SerialName("Balance") val balance: Balance = Balance(),
    @SerialName("PendingCount") val pendingCount: Long = 0,
)

@Serializable
data class CreateWalletResult(
    @SerialName("Mnemonic") val mnemonic: List<String> = emptyList(),
    @SerialName("IdentityPubKey") val identityPubKey: String = "",
    @SerialName("RecoveryRan") val recoveryRan: Boolean = false,
    @SerialName("RecoveredBoardingAddresses") val recoveredBoardingAddresses: Long = 0,
    @SerialName("RecoveredBoardingUTXOs") val recoveredBoardingUtxos: Long = 0,
    @SerialName("RecoveredVTXOs") val recoveredVtxos: Long = 0,
    @SerialName("RecoveredOORReceiveScripts") val recoveredOorReceiveScripts: Long = 0,
    @SerialName("RecoveredOORRecipientEvents") val recoveredOorRecipientEvents: Long = 0,
)

@Serializable
data class UnlockWalletResult(
    @SerialName("IdentityPubKey") val identityPubKey: String = "",
)

@Serializable
data class EntryProgress(
    @SerialName("Phase") val phase: String = "",
    @SerialName("PhaseLabel") val phaseLabel: String = "",
    @SerialName("PaymentHash") val paymentHash: String = "",
    @SerialName("Txid") val txid: String = "",
    @SerialName("ConfirmationHeight") val confirmationHeight: Int = 0,
    @SerialName("VTXOOutpoint") val vtxoOutpoint: String = "",
    @SerialName("Preimage") val preimage: String = "",
)

@Serializable
data class EntryRequest(
    @SerialName("Type") val type: String = "",
    @SerialName("LightningInvoice") val lightningInvoice: String = "",
    @SerialName("PaymentHash") val paymentHash: String = "",
    @SerialName("OnchainAddress") val onchainAddress: String = "",
    @SerialName("ArkAddress") val arkAddress: String = "",
)

@Serializable
data class Entry(
    @SerialName("ID") val id: String = "",
    @SerialName("Kind") val kind: String = "",
    @SerialName("Status") val status: String = "",
    @SerialName("AmountSat") val amountSat: Long = 0,
    @SerialName("FeeSat") val feeSat: Long = 0,
    @SerialName("Counterparty") val counterparty: String = "",
    @SerialName("CreatedAt") val createdAt: String = "",
    @SerialName("UpdatedAt") val updatedAt: String = "",
    @SerialName("Note") val note: String = "",
    @SerialName("FailureReason") val failureReason: String = "",
    @SerialName("Cursor") val cursor: Long = 0,
    @SerialName("Progress") val progress: EntryProgress? = null,
    @SerialName("Request") val request: EntryRequest? = null,
    @SerialName("FailureCode") val failureCode: String = "",
)

@Serializable
data class ReceiveResult(
    @SerialName("Invoice") val invoice: String = "",
    @SerialName("Entry") val entry: Entry = Entry(),
)

@Serializable
data class DepositResult(
    @SerialName("Address") val address: String = "",
    @SerialName("Entry") val entry: Entry = Entry(),
)

@Serializable
data class PrepareSendResult(
    @SerialName("SendIntentID") val sendIntentId: String = "",
    @SerialName("AmountSat") val amountSat: Long = 0,
    @SerialName("ExpectedFeeSat") val expectedFeeSat: Long = 0,
    @SerialName("FeeKnown") val feeKnown: Boolean = false,
    @SerialName("ExpectedTotalOutflowSat") val expectedTotalOutflowSat: Long = 0,
    @SerialName("TotalOutflowKnown") val totalOutflowKnown: Boolean = false,
    @SerialName("Rail") val rail: String = "",
    @SerialName("QuoteStatus") val quoteStatus: String = "",
    @SerialName("DestinationSummary") val destinationSummary: String = "",
    @SerialName("InvoiceDescription") val invoiceDescription: String = "",
    @SerialName("PaymentHash") val paymentHash: String = "",
    @SerialName("Warning") val warning: String = "",
)

@Serializable
data class SendResult(
    @SerialName("Entry") val entry: Entry = Entry(),
    @SerialName("ActualAmountSat") val actualAmountSat: Long = 0,
)

@Serializable
data class ActivityList(
    @SerialName("Entries") val entries: List<Entry> = emptyList(),
    @SerialName("Total") val total: Long = 0,
    @SerialName("HasMore") val hasMore: Boolean = false,
    @SerialName("NextCursor") val nextCursor: String = "",
)

@Serializable
data class ListResult(
    @SerialName("View") val view: String = "",
    @SerialName("Activity") val activity: ActivityList? = null,
)

@Serializable
data class ExitResult(
    @SerialName("Path") val path: String = "",
    @SerialName("Cooperative") val cooperative: Boolean = false,
    @SerialName("QueuedOutpoints") val queuedOutpoints: List<String> = emptyList(),
    @SerialName("Created") val created: Boolean = false,
    @SerialName("ActorID") val actorId: String = "",
    @SerialName("CooperativeError") val cooperativeError: String = "",
)
