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
import kotlinx.coroutines.launch

class GoogleSignInFragment : Fragment() {

    private val viewModel: GoogleSignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_google_signin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Handle Google Sign In Button Click
        view.findViewById<View>(R.id.btn_sign_in_with_google).setOnClickListener {
            startSignInFlow(redirectIfNoAccount = true)
        }

        // Collect UI state exactly once (not per button click)
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
                        is SignInUiState.Loading -> {
                            // Optional: Show loading spinner
                        }
                        is SignInUiState.Idle -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    private fun startSignInFlow(redirectIfNoAccount: Boolean) {
        // Delegate to ViewModel — the getCredential coroutine runs in viewModelScope,
        // so its continuation captures the ViewModel (not this Fragment),
        // preventing the Fragment memory leak via GetCredentialTransport callback chain.
        viewModel.performGetCredential(requireActivity(), redirectIfNoAccount)
    }

    // Launcher to handle return from "Add Account" screen
    private val addAccountLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Retry sign-in when user returns. 
        startSignInFlow(redirectIfNoAccount = false)
    }

    private fun navigateToNextPage() {
        val viewPager = activity?.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        if (viewPager != null) {
            viewPager.currentItem = viewPager.currentItem + 1
        }
    }
}
