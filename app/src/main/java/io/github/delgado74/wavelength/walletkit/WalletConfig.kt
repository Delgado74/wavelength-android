package io.github.delgado74.wavelength.walletkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * JSON config accepted by `Mobile.start`.
 *
 * Null fields are omitted so the embedded daemon keeps its network defaults.
 * See `sdk/wavewalletdk/mobile/config.go` in the wavelength repo.
 */
@Serializable
data class WalletConfig(
    @SerialName("data_dir") val dataDir: String,
    @SerialName("network") val network: String,
    @SerialName("debug_level") val debugLevel: String? = "info",
    @SerialName("allow_mainnet") val allowMainnet: Boolean? = null,
    @SerialName("server_address") val serverAddress: String? = null,
    @SerialName("server_transport") val serverTransport: String? = null,
    @SerialName("server_insecure") val serverInsecure: Boolean? = null,
    @SerialName("wallet_type") val walletType: String? = "lwwallet",
    @SerialName("wallet_esplora_url") val walletEsploraUrl: String? = null,
    @SerialName("wallet_poll_interval_seconds") val walletPollIntervalSeconds: Long? = 30,
    @SerialName("swap_server_address") val swapServerAddress: String? = null,
    @SerialName("swap_server_insecure") val swapServerInsecure: Boolean? = null,
) {
    companion object {
        const val SIGNET = "signet"
        const val MAINNET = "mainnet"
        const val TESTNET4 = "testnet4"

        val supportedNetworks: List<String> = listOf(SIGNET, MAINNET, TESTNET4)

        fun forNetwork(network: String, dataDir: String): WalletConfig = when (network) {
            MAINNET -> WalletConfig(
                dataDir = dataDir,
                network = MAINNET,
                walletEsploraUrl = "https://mempool.space/api",
                allowMainnet = true,
            )

            TESTNET4 -> WalletConfig(
                dataDir = dataDir,
                network = TESTNET4,
                walletEsploraUrl = "https://mempool.space/testnet4/api",
            )

            else -> WalletConfig(
                dataDir = dataDir,
                network = SIGNET,
                walletEsploraUrl = "https://mempool.space/signet/api",
            )
        }
    }
}
