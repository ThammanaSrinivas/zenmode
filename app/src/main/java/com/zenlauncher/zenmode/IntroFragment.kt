package com.zenlauncher.zenmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch

class IntroFragment : Fragment() {

    private val viewModel: GoogleSignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return androidx.compose.ui.platform.ComposeView(requireContext()).apply {
            setContent {
                com.zenlauncher.zenmode.ui.theme.ZenTheme {
                    com.zenlauncher.zenmode.ui.screens.WelcomeScreen(
                        onGoogleSignInClick = {
                            startSignInFlow(redirectIfNoAccount = true)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is SignInUiState.Success -> {
                            navigateToNextPage()
                            viewModel.resetState()
                        }
                        is SignInUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                        is SignInUiState.LaunchIntent -> {
                            addAccountLauncher.launch(state.intent)
                            viewModel.resetState()
                        }
                        is SignInUiState.Loading -> {}
                        is SignInUiState.Idle -> {}
                    }
                }
            }
        }
    }

    private fun startSignInFlow(redirectIfNoAccount: Boolean) {
        viewModel.performGetCredential(requireActivity(), redirectIfNoAccount)
    }

    private val addAccountLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        startSignInFlow(redirectIfNoAccount = false)
    }

    private fun navigateToNextPage() {
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
        if (viewPager != null) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }
}
