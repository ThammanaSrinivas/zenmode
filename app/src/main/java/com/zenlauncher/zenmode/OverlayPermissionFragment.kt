package com.zenlauncher.zenmode

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

class OverlayPermissionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGrant = view.findViewById<android.widget.ImageView>(R.id.btn_action)

        btnGrant.setOnClickListener {
             if (Settings.canDrawOverlays(requireContext())) {
                 // Analytics
                 com.zenlauncher.zenmode.coreapi.services.ServiceLocator.analyticsTracker.trackPermissionsGranted("overlay")

                 // Advance
                 val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                 viewPager.currentItem = viewPager.currentItem + 1
             } else {
                 val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivity(intent)
             }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(requireContext())) {
            val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
            if (viewPager != null) {
                viewPager.post {
                    viewPager.currentItem = viewPager.currentItem + 1
                }
            }
        }
    }
}
