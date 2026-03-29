package com.zenlauncher.zenmode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.ui.screens.AccountabilityScreen
import com.zenlauncher.zenmode.ui.theme.ZenTheme

class AccountabilityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = UsageRepository(this, ServiceLocator.analyticsManager)
        val viewModel = ViewModelProvider(
            this,
            AccountabilityViewModelFactory(repository)
        )[AccountabilityViewModel::class.java]

        setContent {
            ZenTheme(darkTheme = ThemePreferences.isDarkMode(this@AccountabilityActivity)) {
                val uiState by viewModel.uiState.observeAsState(AccountabilityUiState())

                LaunchedEffect(uiState.disconnectResult) {
                    when (val result = uiState.disconnectResult) {
                        is DisconnectResult.Success -> navigateToInviteBuddy()
                        is DisconnectResult.Error -> {
                            Toast.makeText(
                                this@AccountabilityActivity,
                                "Failed to disconnect: ${result.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        null -> Unit
                    }
                }

                AccountabilityScreen(
                    uiState = uiState,
                    onBackClick = { finish() },
                    onCopyCode = { code ->
                        val clipboard = getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("ZenMode Code", code))
                        Toast.makeText(this@AccountabilityActivity, "Code copied!", Toast.LENGTH_SHORT).show()
                    },
                    onBackToHomeClick = { finish() },
                    onChangeBuddyConfirmed = { viewModel.disconnectBuddy() }
                )
            }
        }
    }

    private fun navigateToInviteBuddy() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_BUDDY_CONNECT", true)
        }
        startActivity(intent)
        finish()
    }
}
