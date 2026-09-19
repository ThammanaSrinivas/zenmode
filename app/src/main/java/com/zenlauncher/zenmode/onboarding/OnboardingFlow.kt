package com.zenlauncher.zenmode.onboarding

/**
 * The ZenMode OS onboarding, as data. Kept free of Android types so the routing rules
 * (who sees which step) are unit-testable.
 *
 * New users:        Welcome → Stories → Sign in → Promise → Circle → Permissions → Home apps
 * Returning (v2):   same flow; the Welcome copy changes to "Update now"
 *
 * Sign-in is skipped for anyone already signed in, Circle for anyone who isn't (buddies
 * need an account), and Permissions once every required grant is in place.
 */
enum class OnboardingStep {
    WELCOME,
    STORIES,
    SIGN_IN,
    PROMISE,
    CIRCLE,
    PERMISSIONS,
    HOME_APPS
}

data class OnboardingContext(
    /** Finished the v2 onboarding before, now seeing the ZenMode OS revamp. */
    val isReturningUser: Boolean,
    val isSignedIn: Boolean,
    val hasRequiredPermissions: Boolean,
    /** Opened from home's "Sign in" after onboarding is done: just the sign-in step. */
    val signInOnly: Boolean = false
)

object OnboardingFlow {

    fun steps(context: OnboardingContext): List<OnboardingStep> = buildList {
        if (context.signInOnly) {
            add(OnboardingStep.SIGN_IN)
            return@buildList
        }
        add(OnboardingStep.WELCOME)
        add(OnboardingStep.STORIES)
        if (!context.isSignedIn) add(OnboardingStep.SIGN_IN)
        add(OnboardingStep.PROMISE)
        if (context.isSignedIn) add(OnboardingStep.CIRCLE)
        if (!context.hasRequiredPermissions) add(OnboardingStep.PERMISSIONS)
        add(OnboardingStep.HOME_APPS)
    }

    /**
     * Steps that count toward the progress bar. Welcome is the cover, not a step,
     * so the bar starts on the stories.
     */
    fun progressSteps(steps: List<OnboardingStep>): List<OnboardingStep> =
        steps.filter { it != OnboardingStep.WELCOME }

    /**
     * How far through the flow the user is, 0–100, for the label beside the progress bar.
     * Counts the steps already behind them plus [fraction] of the current one, so the
     * last step reads below 100% until they actually finish.
     */
    fun percentComplete(currentIndex: Int, segments: Int, fraction: Float = 0f): Int {
        if (segments <= 0) return 0
        val done = currentIndex.coerceIn(0, segments) + fraction.coerceIn(0f, 1f)
        return (done / segments * 100f).toInt().coerceIn(0, 100)
    }

    /**
     * Re-evaluates the flow after something changed mid-way (signed in, granted a
     * permission) and returns where [current] now sits. Steps that dropped out behind
     * the user don't move them; if [current] itself dropped out, they land on the step
     * that took its place.
     */
    fun reconcile(
        previous: List<OnboardingStep>,
        current: OnboardingStep,
        context: OnboardingContext
    ): Pair<List<OnboardingStep>, Int> {
        val next = steps(context)
        val index = next.indexOf(current).takeIf { it >= 0 }
            ?: previous.drop(previous.indexOf(current) + 1).firstNotNullOfOrNull { later ->
                next.indexOf(later).takeIf { it >= 0 }
            }
            ?: next.lastIndex
        return next to index
    }

    /** Restores a saved position, clamped to the flow the user has now. */
    fun restoreIndex(steps: List<OnboardingStep>, savedStep: String?): Int =
        savedStep
            ?.let { name -> OnboardingStep.entries.firstOrNull { it.name == name } }
            ?.let { steps.indexOf(it) }
            ?.takeIf { it >= 0 }
            ?: 0
}
