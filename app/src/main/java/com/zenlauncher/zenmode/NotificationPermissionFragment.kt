package com.zenlauncher.zenmode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class NotificationPermissionFragment : Fragment() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
             // Analytics
             ServiceLocator.analyticsTracker.trackPermissionsGranted("notifications")

             val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
             viewPager.currentItem = viewPager.currentItem + 1
        } else {
            // Explain or just stay? For onboarding, we stay or let them pass manually?
            // User can click button again to retry or go to settings.
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnAction = view.findViewById<ImageView>(R.id.btn_action)
        btnAction.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                     // Analytics
                     ServiceLocator.analyticsTracker.trackPermissionsGranted("notifications")

                     val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                     viewPager.currentItem = viewPager.currentItem + 1
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                 // Pre-Tiramisu, notifications are enabled by default usually, or we open settings.
                 // Just advance
                 val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                 viewPager.currentItem = viewPager.currentItem + 1
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                 // Can change button UI? It's an image.
                 // Maybe auto advance or just let user click.
            }
        }
    }
}
