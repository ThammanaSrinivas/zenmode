package com.zenlauncher.zenmode.onboarding

import com.zenlauncher.zenmode.onboarding.OnboardingStep.CIRCLE
import com.zenlauncher.zenmode.onboarding.OnboardingStep.HOME_APPS
import com.zenlauncher.zenmode.onboarding.OnboardingStep.PERMISSIONS
import com.zenlauncher.zenmode.onboarding.OnboardingStep.PROMISE
import com.zenlauncher.zenmode.onboarding.OnboardingStep.SIGN_IN
import com.zenlauncher.zenmode.onboarding.OnboardingStep.STORIES
import com.zenlauncher.zenmode.onboarding.OnboardingStep.WELCOME
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowTest {

    private val newUser = OnboardingContext(isReturningUser = false, isSignedIn = false, hasRequiredPermissions = false)

    @Test
    fun `new signed-out user skips the circle until they have an account`() {
        assertEquals(
            listOf(WELCOME, STORIES, SIGN_IN, PROMISE, PERMISSIONS, HOME_APPS),
            OnboardingFlow.steps(newUser)
        )
    }

    @Test
    fun `signed-in new user gets the circle and no sign-in`() {
        assertEquals(
            listOf(WELCOME, STORIES, PROMISE, CIRCLE, PERMISSIONS, HOME_APPS),
            OnboardingFlow.steps(newUser.copy(isSignedIn = true))
        )
    }

    @Test
    fun `signed-in returning user skips sign-in and permissions already granted`() {
        val returning = OnboardingContext(isReturningUser = true, isSignedIn = true, hasRequiredPermissions = true)
        assertEquals(listOf(WELCOME, STORIES, PROMISE, CIRCLE, HOME_APPS), OnboardingFlow.steps(returning))
    }

    @Test
    fun `signed-out returning user still gets the sign-in step`() {
        val returning = OnboardingContext(isReturningUser = true, isSignedIn = false, hasRequiredPermissions = true)
        assertEquals(listOf(WELCOME, STORIES, SIGN_IN, PROMISE, HOME_APPS), OnboardingFlow.steps(returning))
    }

    @Test
    fun `sign-in-only visit is just the sign-in step`() {
        assertEquals(listOf(SIGN_IN), OnboardingFlow.steps(newUser.copy(isReturningUser = true, signInOnly = true)))
    }

    @Test
    fun `welcome is not part of the progress bar`() {
        assertEquals(
            listOf(STORIES, SIGN_IN, PROMISE, PERMISSIONS, HOME_APPS),
            OnboardingFlow.progressSteps(OnboardingFlow.steps(newUser))
        )
    }

    @Test
    fun `signing in on the sign-in step lands on the step that replaced it`() {
        val before = OnboardingFlow.steps(newUser)
        val (after, index) = OnboardingFlow.reconcile(before, SIGN_IN, newUser.copy(isSignedIn = true))
        assertEquals(PROMISE, after[index])
        assertEquals(CIRCLE, after[index + 1])
    }

    @Test
    fun `re-planning keeps the user on a step that still exists`() {
        val before = OnboardingFlow.steps(newUser)
        val (after, index) = OnboardingFlow.reconcile(before, HOME_APPS, newUser.copy(isSignedIn = true))
        assertEquals(HOME_APPS, after[index])
    }

    @Test
    fun `restore falls back to the start for unknown or dropped steps`() {
        val steps = OnboardingFlow.steps(newUser.copy(isSignedIn = true))
        assertEquals(steps.indexOf(PROMISE), OnboardingFlow.restoreIndex(steps, "PROMISE"))
        assertEquals(0, OnboardingFlow.restoreIndex(steps, "SIGN_IN"))
        assertEquals(0, OnboardingFlow.restoreIndex(steps, "NOT_A_STEP"))
        assertEquals(0, OnboardingFlow.restoreIndex(steps, null))
    }

    @Test
    fun `percent counts finished steps plus progress into the current one`() {
        assertEquals(0, OnboardingFlow.percentComplete(currentIndex = 0, segments = 5))
        assertEquals(10, OnboardingFlow.percentComplete(currentIndex = 0, segments = 5, fraction = 0.5f))
        assertEquals(80, OnboardingFlow.percentComplete(currentIndex = 4, segments = 5))
        assertEquals(100, OnboardingFlow.percentComplete(currentIndex = 4, segments = 5, fraction = 1f))
    }

    @Test
    fun `percent stays in range for odd inputs`() {
        assertEquals(0, OnboardingFlow.percentComplete(currentIndex = 0, segments = 0))
        assertEquals(100, OnboardingFlow.percentComplete(currentIndex = 9, segments = 5, fraction = 3f))
        assertEquals(0, OnboardingFlow.percentComplete(currentIndex = -2, segments = 5, fraction = -1f))
    }
}
