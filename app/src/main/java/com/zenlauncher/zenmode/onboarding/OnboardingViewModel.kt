package com.zenlauncher.zenmode.onboarding

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.zenlauncher.zenmode.AppGridPreferences
import com.zenlauncher.zenmode.DistractingAppsRepository
import com.zenlauncher.zenmode.PromisePreferences
import com.zenlauncher.zenmode.coreapi.UsageRepository
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class OnboardingUiState(
    val steps: List<OnboardingStep>,
    val index: Int,
    val isReturningUser: Boolean,
    val isSignedIn: Boolean,
    val userCode: String?,
    val promiseHours: Int,
    val granted: Set<ZenPermission>,
    val isDefaultLauncher: Boolean,
    val apps: List<HomeAppOption>? = null,
    val selectedApps: List<String> = emptyList(),
    val connectedBuddyName: String? = null,
    val signingIn: Boolean = false,
    val signInOnly: Boolean = false,
    /** Onboarding is saved; hand over to the home screen (which plays "Entering ZenMode"). */
    val finished: Boolean = false
) {
    val step: OnboardingStep get() = steps[index]

    /**
     * Where [of] sits among the steps that show a progress bar (everything but Welcome).
     * Taken per step so a screen animating out keeps its own position.
     */
    fun progressFor(of: OnboardingStep): StepProgress {
        val bar = OnboardingFlow.progressSteps(steps)
        return StepProgress(segments = bar.size, currentIndex = bar.indexOf(of).coerceAtLeast(0))
    }
}

class OnboardingViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        /** Intent extra: open just the sign-in step (home's "Sign in"), then return home. */
        const val EXTRA_SIGN_IN_ONLY = "sign_in_only"
    }

    private val context: Context get() = getApplication()
    private val repository = UsageRepository(application, ServiceLocator.analyticsManager)

    private val _uiState: MutableStateFlow<OnboardingUiState>
    val uiState: StateFlow<OnboardingUiState> get() = _uiState.asStateFlow()

    init {
        if (repository.getOnboardingStartTime() == 0L) {
            repository.setOnboardingStartTime(System.currentTimeMillis())
        }
        val signInOnly = savedStateHandle.get<Boolean>(EXTRA_SIGN_IN_ONLY) == true &&
            repository.isOnboardingComplete() && repository.isOsOnboardingComplete()
        val flowContext = currentContext(isReturningUser = repository.isOnboardingComplete())
            .copy(signInOnly = signInOnly)
        val steps = OnboardingFlow.steps(flowContext)
        _uiState = MutableStateFlow(
            OnboardingUiState(
                steps = steps,
                index = OnboardingFlow.restoreIndex(steps, repository.getOnboardingCurrentStep()),
                isReturningUser = flowContext.isReturningUser,
                isSignedIn = flowContext.isSignedIn,
                userCode = currentUserCode(),
                promiseHours = OnboardingDraft.getPromiseHours(application) ?: PromisePreferences.getDailyHours(application),
                granted = ZenPermission.grantedSet(application),
                isDefaultLauncher = isDefaultLauncher(),
                signInOnly = signInOnly
            )
        )
        if (!signInOnly) loadApps()
    }

    // ── Navigation ────────────────────────────────────────────────

    fun next() {
        val state = _uiState.value
        if (state.step == OnboardingStep.WELCOME && !repository.isOnboardingStartedTracked()) {
            ServiceLocator.analyticsTracker.trackOnboardingStarted()
            repository.setOnboardingStartedTracked(true)
        }
        if (state.index < state.steps.lastIndex) moveTo(state.index + 1)
    }

    /** Returns false at the first step so the host can let the system handle back. */
    fun back(): Boolean {
        val state = _uiState.value
        if (state.index == 0) return false
        moveTo(state.index - 1)
        return true
    }

    private fun moveTo(index: Int) {
        _uiState.update { it.copy(index = index) }
        val step = _uiState.value.step
        repository.setOnboardingCurrentStep(step.name)
        if (step == OnboardingStep.PERMISSIONS) {
            ServiceLocator.analyticsTracker.trackPermissionScreenViewed("checklist")
        }
        if (step == OnboardingStep.HOME_APPS) {
            ServiceLocator.analyticsTracker.trackPermissionScreenViewed("launcher_set_default")
        }
    }

    /**
     * Re-reads everything that can change behind the app's back — grants, default home,
     * sign-in — and re-plans the flow around the current step. Call on every resume.
     */
    fun refreshDeviceState() {
        val before = _uiState.value
        val granted = ZenPermission.grantedSet(context)
        (granted - before.granted).forEach { permission ->
            ServiceLocator.analyticsTracker.trackPermissionGranted(permission.analyticsKey)
            repository.recordPermissionGranted(permission.analyticsKey)
        }
        val signedIn = ServiceLocator.authProvider.isSignedIn()
        // Once the checklist is in the plan it stays for the session, so the user sees every
        // row tick and can step back to it; it's only added if it wasn't planned.
        val flowContext = OnboardingContext(
            isReturningUser = before.isReturningUser,
            isSignedIn = signedIn,
            hasRequiredPermissions = OnboardingStep.PERMISSIONS !in before.steps &&
                ZenPermission.hasAllRequired(granted),
            signInOnly = before.signInOnly
        )
        val (steps, index) = OnboardingFlow.reconcile(before.steps, before.step, flowContext)
        _uiState.update {
            it.copy(
                steps = steps,
                index = index,
                isSignedIn = signedIn,
                userCode = currentUserCode(),
                granted = granted,
                isDefaultLauncher = isDefaultLauncher()
            )
        }
        if (steps[index] != before.step) repository.setOnboardingCurrentStep(steps[index].name)

        // Becoming the home app restarts onboarding in a new home task, dropping the result
        // callback; the draft flag lets that fresh instance's first resume finish the job.
        if (OnboardingDraft.isAwaitingDefaultLauncher(context) && _uiState.value.isDefaultLauncher) {
            complete()
        }
    }

    /** The user asked to make ZenMode the home app; finish as soon as that's true. */
    fun onDefaultLauncherRequested() {
        OnboardingDraft.setAwaitingDefaultLauncher(context, true)
    }

    // ── Sign in ───────────────────────────────────────────────────

    fun setSigningIn(signingIn: Boolean) = _uiState.update { it.copy(signingIn = signingIn) }

    /** Signed in: the sign-in step drops out of the plan, landing the user on the next one. */
    fun onSignedIn() {
        _uiState.update { it.copy(signingIn = false, finished = it.signInOnly) }
        refreshDeviceState()
    }

    // ── Promise, circle, apps ─────────────────────────────────────

    fun setPromiseHours(hours: Int) {
        OnboardingDraft.setPromiseHours(context, hours)
        _uiState.update { it.copy(promiseHours = hours) }
    }

    fun onBuddyConnected(name: String) = _uiState.update { it.copy(connectedBuddyName = name) }

    fun toggleApp(packageName: String) {
        _uiState.update { it.copy(selectedApps = HomeAppSuggestions.toggle(it.selectedApps, packageName)) }
        OnboardingDraft.setSelectedApps(context, _uiState.value.selectedApps)
    }

    private fun loadApps() {
        viewModelScope.launch {
            val (apps, suggested) = withContext(Dispatchers.IO) { queryLaunchableApps() }
            _uiState.update { state ->
                // v2 pins (max 4) keep their places up front; suggestions fill the rest.
                val installed = apps.map { it.packageName }.toSet()
                val restored = OnboardingDraft.getSelectedApps(context)?.filter { it in installed }
                val existingPins = repository.getPinnedApps().filter { it in installed }
                state.copy(
                    apps = apps,
                    selectedApps = restored
                        ?: (existingPins + suggested).distinct().take(HomeAppSuggestions.HOME_APP_LIMIT)
                )
            }
            if (!_uiState.value.finished) OnboardingDraft.setSelectedApps(context, _uiState.value.selectedApps)
        }
    }

    private fun queryLaunchableApps(): Pair<List<HomeAppOption>, List<String>> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val iconPx = (48 * context.resources.displayMetrics.density).toInt()
        val apps = pm.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                HomeAppOption(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()

        val distracting = apps.map { it.packageName }
            .filter { DistractingAppsRepository.isDistracting(context, pm, it) }
            .toSet()
        val suggested = HomeAppSuggestions.suggest(
            installed = apps.map { HomeAppSuggestions.Candidate(it.packageName, it.label) },
            distracting = distracting,
            usageMinutes = weeklyUsageMinutes()
        )
        return apps to suggested
    }

    /** Last 7 days of foreground minutes per app, or empty without usage access. */
    private fun weeklyUsageMinutes(): Map<String, Long> {
        if (ZenPermission.USAGE_ACCESS !in _uiState.value.granted) return emptyMap()
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val end = System.currentTimeMillis()
        return runCatching {
            usm.queryAndAggregateUsageStats(end - TimeUnit.DAYS.toMillis(7), end)
                .mapValues { TimeUnit.MILLISECONDS.toMinutes(it.value.totalTimeInForeground) }
        }.getOrDefault(emptyMap())
    }

    // ── Finish ────────────────────────────────────────────────────

    /** Saves everything the flow collected and queues the celebration on the home screen. */
    fun complete() {
        val state = _uiState.value
        if (state.finished) return

        PromisePreferences.setDailyHours(context, state.promiseHours)
        // A fresh instance may finish before its app list loads; the draft has the picks.
        val selectedApps = state.selectedApps.ifEmpty { OnboardingDraft.getSelectedApps(context).orEmpty() }
        if (selectedApps.isNotEmpty()) {
            repository.savePinnedApps(selectedApps)
            if (AppGridPreferences.getAppCount(context) < selectedApps.size) {
                AppGridPreferences.setAppCount(context, HomeAppSuggestions.HOME_APP_LIMIT)
            }
        }

        if (state.isDefaultLauncher) {
            repository.recordPermissionGranted("launcher_set_default")
            ServiceLocator.analyticsTracker.trackPermissionGranted("launcher_set_default")
        }
        val startTime = repository.getOnboardingStartTime()
        val timeTakenSec = if (startTime > 0) ((System.currentTimeMillis() - startTime) / 1000).toInt() else 0
        ServiceLocator.analyticsTracker.trackSetupCompleted(timeTakenSec, repository.getGrantedPermissionsCount())
        repository.clearOnboardingMetrics()

        repository.setOnboardingComplete(true)
        repository.setOsOnboardingComplete(true)
        repository.clearOnboardingCurrentStep()
        repository.setEnteringCelebrationPending(true)
        OnboardingDraft.clear(context)

        _uiState.update { it.copy(finished = true) }
    }

    // ── Device reads ──────────────────────────────────────────────

    private fun currentContext(isReturningUser: Boolean) = OnboardingContext(
        isReturningUser = isReturningUser,
        isSignedIn = ServiceLocator.authProvider.isSignedIn(),
        hasRequiredPermissions = ZenPermission.hasAllRequired(ZenPermission.grantedSet(context))
    )

    private fun currentUserCode(): String? =
        repository.getUserUid() ?: ServiceLocator.authProvider.getCurrentUserId()

    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == context.packageName
    }
}
