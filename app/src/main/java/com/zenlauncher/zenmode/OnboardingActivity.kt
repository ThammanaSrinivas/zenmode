package com.zenlauncher.zenmode

import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import com.zenlauncher.zenmode.ui.components.rememberZenFeedback
import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import com.zenlauncher.zenmode.onboarding.CircleStep
import com.zenlauncher.zenmode.onboarding.HomeAppsStep
import com.zenlauncher.zenmode.onboarding.OnboardingStep
import com.zenlauncher.zenmode.onboarding.OnboardingUiState
import com.zenlauncher.zenmode.onboarding.OnboardingViewModel
import com.zenlauncher.zenmode.onboarding.PermissionsStep
import com.zenlauncher.zenmode.onboarding.PromiseStep
import com.zenlauncher.zenmode.onboarding.SignInStep
import com.zenlauncher.zenmode.onboarding.StoriesStep
import com.zenlauncher.zenmode.onboarding.WelcomeStep
import com.zenlauncher.zenmode.onboarding.ZenPermission
import com.zenlauncher.zenmode.ui.screens.AccessibilityDisclosureScreen
import com.zenlauncher.zenmode.ui.screens.BuddyAddResult
import com.zenlauncher.zenmode.ui.theme.LightOnly
import com.zenlauncher.zenmode.ui.theme.ZenTheme
import kotlinx.coroutines.launch

/**
 * ZenMode OS onboarding. The flow itself is [OnboardingViewModel]; this activity only
 * performs what needs an Activity — sign-in, Settings round-trips, the home-role prompt,
 * share sheets — and hands results back.
 */
class OnboardingActivity : ComponentActivity() {

    companion object {
        /** Home's "Sign in": just the sign-in step, then back to home. */
        fun signInIntent(context: android.content.Context): Intent =
            Intent(context, OnboardingActivity::class.java)
                .putExtra(OnboardingViewModel.EXTRA_SIGN_IN_ONLY, true)
    }

    private val viewModel: OnboardingViewModel by viewModels()
    private val signInViewModel: GoogleSignInViewModel by viewModels()
    private val buddyConnector by lazy {
        BuddyConnector(this, UsageRepository(applicationContext, ServiceLocator.analyticsManager))
    }

    private var showAccessibilityDisclosure by mutableStateOf(false)

    private val addAccountLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        signInViewModel.performGetCredential(this, redirectIfNoAccount = false)
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshDeviceState()
    }

    // refreshDeviceState() completes onboarding once ZenMode is the home app.
    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.refreshDeviceState()
        // After repeated "No"s Android stops showing the role dialog and cancels
        // instantly; the Settings page still works, so fall back to it.
        if (!viewModel.isDefaultLauncher() && result.resultCode == Activity.RESULT_CANCELED) openHomeSettings()
    }

    private val homeSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshDeviceState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        observeSignIn()

        setContent {
            // Onboarding is always paper, whatever the phone or a saved Appearance choice says:
            // the first-run screens are designed light. LightOnly also swaps in light resources
            // so colorResource skips values-night.
            ZenTheme(darkTheme = false) {
                LightOnly { OnboardingContent() }
            }
        }
    }

    @Composable
    private fun OnboardingContent() {
        val state by viewModel.uiState.collectAsState()
        LaunchedEffect(state.finished) { if (state.finished) openHome() }
        if (showAccessibilityDisclosure) {
            AccessibilityDisclosureScreen(
                onAccept = {
                    showAccessibilityDisclosure = false
                    openSettings(ZenPermission.ACCESSIBILITY)
                },
                onDecline = { showAccessibilityDisclosure = false }
            )
        } else {
            BackHandler(enabled = state.index > 0 || state.signInOnly) {
                if (state.signInOnly) openHome() else viewModel.back()
            }
            OnboardingSteps(state)
        }
    }

    override fun onResume() {
        super.onResume()
        // Grants and the default home can change in Settings while we're away.
        viewModel.refreshDeviceState()
    }

    @Composable
    private fun OnboardingSteps(state: OnboardingUiState) {
        // Forward a page: one marimba note. Back: a haptic only.
        val feedback = rememberZenFeedback()
        var lastIndex by remember { mutableIntStateOf(state.index) }
        LaunchedEffect(state.index) {
            when {
                state.index > lastIndex -> feedback.step()
                state.index < lastIndex -> feedback.back()
            }
            lastIndex = state.index
        }
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = state.steps.indexOf(targetState) >= state.steps.indexOf(initialState)
                val direction = if (forward) 1 else -1
                (slideInHorizontally(tween(380)) { it * direction / 3 } + fadeIn(tween(380)))
                    .togetherWith(slideOutHorizontally(tween(260)) { -it * direction / 4 } + fadeOut(tween(200)))
            },
            label = "onboardingStep"
        ) { step ->
            val progress = state.progressFor(step)
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    isReturningUser = state.isReturningUser,
                    onContinue = viewModel::next
                )
                OnboardingStep.STORIES -> StoriesStep(
                    progress = progress,
                    onBack = { viewModel.back() },
                    onContinue = viewModel::next
                )
                OnboardingStep.SIGN_IN -> SignInStep(
                    progress = progress,
                    isLoading = state.signingIn,
                    onBack = { if (state.signInOnly) openHome() else viewModel.back() },
                    onGoogleSignIn = { signInViewModel.performGetCredential(this@OnboardingActivity, redirectIfNoAccount = true) },
                    onEmailSignIn = signInViewModel::signInWithEmail,
                    onExplore = { if (state.signInOnly) openHome() else viewModel.next() }
                )
                OnboardingStep.PROMISE -> PromiseStep(
                    progress = progress,
                    dailyHours = state.promiseHours,
                    onDailyHoursChange = viewModel::setPromiseHours,
                    onBack = { viewModel.back() },
                    onContinue = viewModel::next
                )
                OnboardingStep.CIRCLE -> CircleStep(
                    progress = progress,
                    userCode = state.userCode,
                    connectedBuddyName = state.connectedBuddyName,
                    onBack = { viewModel.back() },
                    onShareLink = { state.userCode?.let(buddyConnector::shareBuddyInvite) },
                    onCopyCode = { state.userCode?.let { buddyConnector.copyUserCode(it, showToast = true) } },
                    onAddBuddy = { targetUid ->
                        buddyConnector.addBuddy(targetUid).also { result ->
                            if (result is BuddyAddResult.Success) viewModel.onBuddyConnected(result.buddyName)
                        }
                    },
                    onRandomConnect = {
                        lifecycleScope.launch {
                            buddyConnector.randomConnect()?.let(viewModel::onBuddyConnected)
                        }
                    },
                    onContinue = viewModel::next
                )
                OnboardingStep.PERMISSIONS -> PermissionsStep(
                    progress = progress,
                    permissions = ZenPermission.applicable(),
                    granted = state.granted,
                    onBack = { viewModel.back() },
                    onAllow = ::requestPermission,
                    onContinue = viewModel::next
                )
                OnboardingStep.HOME_APPS -> HomeAppsStep(
                    progress = progress,
                    apps = state.apps,
                    selected = state.selectedApps,
                    isDefaultLauncher = state.isDefaultLauncher,
                    onBack = { viewModel.back() },
                    onToggle = viewModel::toggleApp,
                    onSetDefault = ::setAsDefaultLauncher,
                    onMaybeLater = viewModel::complete
                )
            }
        }
    }

    // ── Sign in ───────────────────────────────────────────────────

    private fun observeSignIn() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                signInViewModel.uiState.collect { state ->
                    when (state) {
                        is SignInUiState.Loading -> viewModel.setSigningIn(true)
                        is SignInUiState.Success -> {
                            viewModel.onSignedIn()
                            signInViewModel.resetState()
                        }
                        is SignInUiState.Error -> {
                            viewModel.setSigningIn(false)
                            Toast.makeText(this@OnboardingActivity, state.message, Toast.LENGTH_LONG).show()
                            signInViewModel.resetState()
                        }
                        is SignInUiState.LaunchIntent -> {
                            addAccountLauncher.launch(state.intent)
                            signInViewModel.resetState()
                        }
                        is SignInUiState.Idle -> Unit
                    }
                }
            }
        }
    }

    // ── Permissions ───────────────────────────────────────────────

    private fun requestPermission(permission: ZenPermission) {
        ServiceLocator.analyticsTracker.trackOnboardingPermissionRequested(permission.name)
        when {
            permission == ZenPermission.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            // Play policy: accessibility needs the prominent disclosure before Settings.
            permission == ZenPermission.ACCESSIBILITY -> showAccessibilityDisclosure = true
            else -> openSettings(permission)
        }
    }

    private fun openSettings(permission: ZenPermission) {
        val intent = permission.settingsIntent(this) ?: return
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    // ── Default launcher & finish ─────────────────────────────────

    private fun setAsDefaultLauncher() {
        if (viewModel.isDefaultLauncher()) {
            viewModel.complete()
            return
        }
        viewModel.onDefaultLauncherRequested()
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) getSystemService(RoleManager::class.java) else null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
            homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            openHomeSettings()
        }
    }

    private fun openHomeSettings() {
        try {
            homeSettingsLauncher.launch(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            homeSettingsLauncher.launch(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun openHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!viewModel.uiState.value.finished && !isChangingConfigurations) {
            ServiceLocator.analyticsTracker.trackOnboardingAbandoned(viewModel.uiState.value.step.name)
        }
    }
}
