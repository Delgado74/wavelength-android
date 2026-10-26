package io.github.delgado74.wavelength.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.delgado74.wavelength.BuildConfig
import io.github.delgado74.wavelength.Phase
import io.github.delgado74.wavelength.UiState
import io.github.delgado74.wavelength.WalletViewModel
import io.github.delgado74.wavelength.walletkit.Entry
import io.github.delgado74.wavelength.walletkit.PrepareSendResult
import io.github.delgado74.wavelength.walletkit.SendResult
import io.github.delgado74.wavelength.walletkit.WalletConfig
import io.github.delgado74.wavelength.walletkit.WalletState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AppRoot(vm: WalletViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(4_000)
            vm.clearMessage()
        }
    }

    when (state.phase) {
        Phase.STARTING -> LoadingScreen()
        Phase.ERROR -> ErrorScreen(state.error, vm::retry)
        Phase.ONBOARDING -> OnboardingScreen(state, vm)
        Phase.BACKUP -> BackupScreen(state.mnemonic, vm::confirmBackup)
        Phase.UNLOCK -> UnlockScreen(state, vm)
        Phase.READY -> MainScreen(state, vm)
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = BitcoinOrange)
        Spacer(Modifier.height(24.dp))
        Text("Iniciando wallet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "La primera sincronización puede tardar unos segundos.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorScreen(error: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("No se pudo iniciar la wallet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            error ?: "Error desconocido",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun OnboardingScreen(state: UiState, vm: WalletViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    when (step) {
        1 -> CreateWalletStep(
            state = state,
            onBack = { step = 0 },
            onCreate = vm::createWallet,
        )

        2 -> RestoreWalletStep(
            state = state,
            onBack = { step = 0 },
            onRestore = vm::restoreWallet,
        )

        else -> WelcomeStep(
            state = state,
            onCreate = { step = 1 },
            onRestore = { step = 2 },
            onNetwork = vm::selectNetwork,
        )
    }
}

@Composable
private fun WelcomeStep(
    state: UiState,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    onNetwork: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            "Wavelength",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = BitcoinOrange,
        )
        Text(
            "Bitcoin autocustodiado con Ark y Lightning",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Text("Red", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WalletConfig.supportedNetworks.forEach { network ->
                FilterChip(
                    selected = state.network == network,
                    onClick = { onNetwork(network) },
                    enabled = !state.busy,
                    label = { Text(networkLabel(network)) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = onCreate,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Crear wallet nueva")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRestore,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restaurar wallet")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Eres el único responsable de tu frase semilla. Esta app no custodia tus fondos.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ErrorBanner(state.error)
    }
}

@Composable
private fun CreateWalletStep(
    state: UiState,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val valid = password.length >= 8 && password == confirm

    ScreenScaffold(title = "Crear wallet", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Elige una contraseña para cifrar tu wallet en este dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PasswordField(password, { password = it }, "Contraseña")
            PasswordField(confirm, { confirm = it }, "Repetir contraseña", ImeAction.Done)
            Text(
                "Mínimo 8 caracteres. No podemos recuperarla si la olvidas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (password.isNotEmpty() && confirm.isNotEmpty() && password != confirm) {
                ErrorBanner("Las contraseñas no coinciden")
            }
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Button(
                onClick = { onCreate(password) },
                enabled = valid && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Crear wallet")
            }
            ErrorBanner(state.error)
            Spacer(Modifier.height(16.dp))
            Text(
                "Se generará una frase semilla de 24 palabras. Guárdala offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RestoreWalletStep(
    state: UiState,
    onBack: () -> Unit,
    onRestore: (List<String>, String) -> Unit,
) {
    var words by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val wordList = words.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val valid = wordList.size == 24 && password.length >= 8 && password == confirm

    ScreenScaffold(title = "Restaurar wallet", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Ingresa las 24 palabras de tu frase semilla separadas por espacios.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = words,
                onValueChange = { words = it },
                label = { Text("Frase semilla") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${wordList.size}/24 palabras",
                style = MaterialTheme.typography.labelSmall,
                color = if (wordList.size == 24) PositiveGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PasswordField(password, { password = it }, "Contraseña")
            PasswordField(confirm, { confirm = it }, "Repetir contraseña", ImeAction.Done)
            if (password.isNotEmpty() && confirm.isNotEmpty() && password != confirm) {
                ErrorBanner("Las contraseñas no coinciden")
            }
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Restaurando y sincronizando. Esto puede tardar varios minutos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onRestore(wordList, password) },
                enabled = valid && !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restaurar wallet")
            }
            ErrorBanner(state.error)
        }
    }
}

@Composable
private fun UnlockScreen(state: UiState, vm: WalletViewModel) {
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Wavelength",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = BitcoinOrange,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Wallet bloqueada",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        NetworkPill(state.network)
        Spacer(Modifier.height(24.dp))
        PasswordField(password, { password = it }, "Contraseña", ImeAction.Done)
        Spacer(Modifier.height(12.dp))
        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = { vm.unlockWallet(password) },
            enabled = password.isNotEmpty() && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Desbloquear")
        }
        ErrorBanner(state.error)
        Spacer(Modifier.height(16.dp))
        Text(
            "La contraseña solo se usa para desbloquear tu wallet en este dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupScreen(mnemonic: List<String>, onConfirm: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var accepted by rememberSaveable { mutableStateOf(false) }
    val numbered = mnemonic.mapIndexed { index, word -> "${index + 1}. $word" }.joinToString("\n")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Respalda tu frase", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Estas 24 palabras son la única forma de recuperar tu wallet. Escríbelas en papel y guárdalas offline. No las compartas con nadie.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                mnemonic.chunked(2).forEachIndexed { rowIndex, pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEachIndexed { columnIndex, word ->
                            WordChip(
                                index = rowIndex * 2 + columnIndex + 1,
                                word = word,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        TextButton(onClick = { clipboard.setText(AnnotatedString(numbered)) }) {
            Text("Copiar frase")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = accepted, onCheckedChange = { accepted = it })
            Text(
                "Guardé mis 24 palabras en un lugar seguro",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onConfirm,
            enabled = accepted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continuar")
        }
        Text(
            "Nunca compartas esta frase. Cualquiera que la tenga puede gastar tus fondos.",
            style = MaterialTheme.typography.bodySmall,
            color = WarningAmber,
        )
    }
}

@Composable
private fun WordChip(index: Int, word: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$index",
                style = MaterialTheme.typography.labelSmall,
                color = BitcoinOrange,
                modifier = Modifier.width(22.dp),
            )
            Text(
                word,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun MainScreen(state: UiState, vm: WalletViewModel) {
    var route by rememberSaveable { mutableStateOf("main") }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = route != "main") {
        vm.clearQuote()
        vm.clearLastSend()
        route = "main"
    }

    when (route) {
        "receive" -> ReceiveScreen(state, vm, initialOnchain = false, onBack = { route = "main" })
        "deposit" -> ReceiveScreen(state, vm, initialOnchain = true, onBack = { route = "main" })
        "send" -> SendScreen(state, vm, onBack = {
            vm.clearQuote()
            vm.clearLastSend()
            route = "main"
        })

        else -> MainTabs(
            state = state,
            vm = vm,
            tab = tab,
            onTab = { tab = it },
            onReceive = { route = "receive" },
            onDeposit = { route = "deposit" },
            onSend = { route = "send" },
        )
    }
}

@Composable
private fun MainTabs(
    state: UiState,
    vm: WalletViewModel,
    tab: Int,
    onTab: (Int) -> Unit,
    onReceive: () -> Unit,
    onDeposit: () -> Unit,
    onSend: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { onTab(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Wallet") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { onTab(1) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                    label = { Text("Actividad") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { onTab(2) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> WalletTab(
                state = state,
                vm = vm,
                modifier = Modifier.padding(padding),
                onReceive = onReceive,
                onDeposit = onDeposit,
                onSend = onSend,
            )

            1 -> ActivityTab(state, vm, Modifier.padding(padding))
            else -> SettingsTab(state, vm, Modifier.padding(padding))
        }
    }
}

@Composable
private fun WalletTab(
    state: UiState,
    vm: WalletViewModel,
    modifier: Modifier = Modifier,
    onReceive: () -> Unit,
    onDeposit: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.message?.let { MessageBanner(it) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Wavelength",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BitcoinOrange,
                modifier = Modifier.weight(1f),
            )
            NetworkPill(state.network)
        }
        state.info?.let { info ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(info.serverConnected)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (info.serverConnected) "Conectado al operador Ark" else "Conectando al operador...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BalanceCard(
            confirmed = state.balance.confirmedSat,
            pendingIn = state.balance.pendingInSat,
            pendingOut = state.balance.pendingOutSat,
            pendingCount = state.pendingCount,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onReceive, modifier = Modifier.weight(1f)) {
                Text("Recibir")
            }
            Button(onClick = onSend, modifier = Modifier.weight(1f)) {
                Text("Enviar")
            }
            OutlinedButton(onClick = onDeposit, modifier = Modifier.weight(1f)) {
                Text("Depositar")
            }
        }
        ErrorBanner(state.error)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Actividad reciente",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (state.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = vm::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        }
        if (state.activity.isEmpty()) {
            Text(
                "Todavía no hay movimientos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.activity.take(8).forEach { entry ->
                EntryRow(entry)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ActivityTab(state: UiState, vm: WalletViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Actividad",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = vm::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
        }
        if (state.busy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        ErrorBanner(state.error)
        Spacer(Modifier.height(8.dp))
        if (state.activity.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Sin actividad todavía",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.activity) { entry ->
                    EntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(state: UiState, vm: WalletViewModel, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingRow("Red", networkLabel(state.network))
                state.info?.let { info ->
                    SettingRow("Versión", info.version)
                    SettingRow("Altura de bloque", formatSats(info.blockHeight))
                    SettingRow(
                        "Operador Ark",
                        if (info.serverConnected) "Conectado" else "Conectando",
                    )
                    SettingRow("Endpoint Ark", operatorLabel(state.network))
                    SettingRow("Tipo de wallet", info.walletType)
                    SettingRow("Estado", walletStateLabel(info.walletState))
                }
            }
        }
        state.info?.identityPubKey?.takeIf { it.isNotBlank() }?.let { pubkey ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Identidad", style = MaterialTheme.typography.titleSmall)
                    SelectionContainer {
                        Text(
                            pubkey,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(pubkey)) }) {
                        Text("Copiar identidad")
                    }
                }
            }
        }
        OutlinedButton(
            onClick = vm::lock,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("Bloquear wallet")
        }
        Text(
            "Wavelength Android ${BuildConfig.VERSION_NAME}. Beta para signet. No uses fondos que no puedas perder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiveScreen(
    state: UiState,
    vm: WalletViewModel,
    initialOnchain: Boolean,
    onBack: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(if (initialOnchain) 1 else 0) }
    var amount by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    val amountSat = amount.toLongOrNull() ?: 0L

    LaunchedEffect(tab) {
        vm.clearGenerated()
    }

    ScreenScaffold(title = "Recibir", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Lightning") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("On-chain") })
            }
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (tab == 0) {
                Text(
                    "Genera una factura Lightning para cobrar en tu wallet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountField(amount, { amount = it.filter(Char::isDigit) }, "Monto (sats)")
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { vm.createInvoice(amountSat, memo) },
                    enabled = amountSat > 0 && !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Generar factura")
                }
                state.invoice?.let { InvoiceCard(it) }
            } else {
                Text(
                    "Genera una dirección de depósito on-chain (boarding).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AmountField(
                    amount,
                    { amount = it.filter(Char::isDigit) },
                    "Monto aproximado (opcional)",
                )
                Button(
                    onClick = { vm.createDeposit(amountSat) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Generar dirección")
                }
                state.depositAddress?.let { AddressCard(it) }
            }
            ErrorBanner(state.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendScreen(state: UiState, vm: WalletViewModel, onBack: () -> Unit) {
    var destination by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var maxFee by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    val dest = destination.trim()
    val isInvoice = dest.lowercase(Locale.US).startsWith("ln")
    val amountSat = amount.toLongOrNull() ?: 0L
    val canPrepare = dest.isNotEmpty() &&
        (isInvoice || amountSat > 0) &&
        state.quote == null &&
        state.lastSend == null &&
        !state.busy

    ScreenScaffold(title = "Enviar", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            state.lastSend?.let { result ->
                SendSuccessCard(result)
                TextButton(onClick = {
                    vm.clearLastSend()
                    onBack()
                }) {
                    Text("Volver a la wallet")
                }
            }
            if (state.lastSend == null) {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Factura o dirección") },
                    placeholder = { Text("lnbc... o bc1...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isInvoice && dest.isNotEmpty()) {
                    AmountField(amount, { amount = it.filter(Char::isDigit) }, "Monto (sats)")
                }
                AmountField(
                    maxFee,
                    { maxFee = it.filter(Char::isDigit) },
                    "Comisión máxima (sats, opcional)",
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        vm.prepareSend(
                            invoice = if (isInvoice) dest else null,
                            onchainAddress = if (isInvoice) null else dest,
                            amountSat = if (isInvoice) 0L else amountSat,
                            note = note,
                            maxFeeSat = maxFee.toLongOrNull() ?: 0L,
                        )
                    },
                    enabled = canPrepare,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cotizar pago")
                }
                state.quote?.let { quote ->
                    QuoteCard(
                        quote = quote,
                        busy = state.busy,
                        onConfirm = vm::confirmSend,
                        onCancel = vm::clearQuote,
                    )
                }
            }
            ErrorBanner(state.error)
        }
    }
}

@Composable
private fun BalanceCard(
    confirmed: Long,
    pendingIn: Long,
    pendingOut: Long,
    pendingCount: Long,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "BALANCE CONFIRMADO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${formatSats(confirmed)} sats",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            if (pendingIn > 0) {
                Text(
                    "+${formatSats(pendingIn)} sats entrantes pendientes",
                    style = MaterialTheme.typography.bodySmall,
                    color = PositiveGreen,
                )
            }
            if (pendingOut > 0) {
                Text(
                    "-${formatSats(pendingOut)} sats salientes pendientes",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningAmber,
                )
            }
            if (pendingCount > 0) {
                Text(
                    "$pendingCount operaciones en curso",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry) {
    val incoming = entry.kind == "receive" || entry.kind == "deposit"
    val sign = if (incoming) "+" else "-"
    val amountColor = if (incoming) PositiveGreen else NegativeRed
    val subtitle = entry.note.ifBlank { entry.counterparty }
        .ifBlank { entry.progress?.phaseLabel ?: "" }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    kindLabel(entry.kind),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        shortText(subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$sign${formatSats(entry.amountSat)} sats",
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                )
                Text(
                    statusLabel(entry.status),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(entry.status),
                )
            }
        }
        Text(
            formatDate(entry.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun QuoteCard(
    quote: PrepareSendResult,
    busy: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Cotización", style = MaterialTheme.typography.titleMedium)
            SettingRow("Monto", "${formatSats(quote.amountSat)} sats")
            if (quote.feeKnown) {
                SettingRow("Comisión estimada", "${formatSats(quote.expectedFeeSat)} sats")
            }
            if (quote.totalOutflowKnown) {
                SettingRow("Total", "${formatSats(quote.expectedTotalOutflowSat)} sats")
            }
            if (quote.rail.isNotBlank()) {
                SettingRow("Ruta", railLabel(quote.rail))
            }
            if (quote.destinationSummary.isNotBlank()) {
                Text(
                    quote.destinationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (quote.warning.isNotBlank()) {
                Text(
                    quote.warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningAmber,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Confirmar")
                }
            }
        }
    }
}

@Composable
private fun SendSuccessCard(result: SendResult) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Pago enviado",
                style = MaterialTheme.typography.titleMedium,
                color = PositiveGreen,
            )
            SettingRow("Monto", "${formatSats(result.actualAmountSat)} sats")
            if (result.entry.feeSat > 0) {
                SettingRow("Comisión", "${formatSats(result.entry.feeSat)} sats")
            }
            Text(
                "Puede tardar unos segundos en confirmarse en la actividad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InvoiceCard(invoice: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Factura Lightning", style = MaterialTheme.typography.titleSmall)
            SelectionContainer {
                Text(
                    invoice,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row {
                TextButton(onClick = { clipboard.setText(AnnotatedString(invoice)) }) {
                    Text("Copiar")
                }
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, invoice)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir factura"))
                }) {
                    Text("Compartir")
                }
            }
        }
    }
}

@Composable
private fun AddressCard(address: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Dirección on-chain", style = MaterialTheme.typography.titleSmall)
            SelectionContainer {
                Text(
                    address,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row {
                TextButton(onClick = { clipboard.setText(AnnotatedString(address)) }) {
                    Text("Copiar")
                }
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, address)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir dirección"))
                }) {
                    Text("Compartir")
                }
            }
        }
    }
}

@Composable
private fun StatusDot(connected: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                if (connected) PositiveGreen else WarningAmber,
                CircleShape,
            ),
    )
}

@Composable
private fun NetworkPill(network: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            networkLabel(network).uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            color = BitcoinOrange,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun MessageBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun ErrorBanner(error: String?) {
    if (error.isNullOrBlank()) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    onBack?.let { back ->
                        IconButton(onClick = back) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        content = content,
    )
}

private fun networkLabel(network: String): String = when (network) {
    WalletConfig.MAINNET -> "Mainnet"
    WalletConfig.TESTNET4 -> "Testnet4"
    else -> "Signet"
}

private fun operatorLabel(network: String): String = when (network) {
    WalletConfig.MAINNET -> "wavelength.lightning.finance:443"
    WalletConfig.TESTNET4 -> "lumosd-testnet4.testnet.lightningcluster.com:443"
    else -> "signet.wavelength.lightning.finance:443"
}

private fun walletStateLabel(state: WalletState): String = when (state) {
    WalletState.READY -> "Lista"
    WalletState.SYNCING -> "Sincronizando"
    WalletState.LOCKED -> "Bloqueada"
    WalletState.NONE -> "Sin crear"
    WalletState.UNSPECIFIED -> "Desconocida"
}

private fun kindLabel(kind: String): String = when (kind) {
    "send" -> "Enviado"
    "receive" -> "Recibido"
    "deposit" -> "Depósito"
    "exit" -> "Salida"
    else -> kind.replaceFirstChar { it.uppercase(Locale.US) }
}

private fun statusLabel(status: String): String = when (status) {
    "complete" -> "Completado"
    "failed" -> "Fallido"
    "pending" -> "Pendiente"
    else -> status
}

private fun statusColor(status: String): Color = when (status) {
    "complete" -> PositiveGreen
    "failed" -> NegativeRed
    else -> WarningAmber
}

private fun railLabel(rail: String): String = when (rail) {
    "in_ark" -> "Ark"
    "lightning" -> "Lightning"
    "onchain" -> "On-chain"
    "credit" -> "Crédito"
    "mixed" -> "Mixta"
    "offchain_unknown" -> "Off-chain"
    else -> rail
}

private fun formatSats(value: Long): String = String.format(Locale.US, "%,d", value)

private fun formatDate(value: String): String =
    if (value.length >= 16) value.replace('T', ' ').take(16) else value

private fun shortText(value: String): String =
    if (value.length <= 40) value else value.take(18) + "..." + value.takeLast(10)
