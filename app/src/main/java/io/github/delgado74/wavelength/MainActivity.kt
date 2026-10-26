package io.github.delgado74.wavelength

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.delgado74.wavelength.ui.AppRoot
import io.github.delgado74.wavelength.ui.WavelengthTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WavelengthTheme {
                AppRoot(viewModel)
            }
        }
    }
}
