package com.zenlauncher.zenmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class BatteryWidgetFragment : Fragment(R.layout.fragment_widget_battery) {

    private lateinit var textBattery: TextView
    private lateinit var progressBattery: ProgressBar

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateBatteryUI(it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textBattery = view.findViewById(R.id.textBattery)
        progressBattery = view.findViewById(R.id.progressBattery)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        androidx.core.content.ContextCompat.registerReceiver(
            requireContext(), batteryReceiver, filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(batteryReceiver)
    }

    private fun updateBatteryUI(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()

        textBattery.text = "${batteryPct.toInt()}%"
        progressBattery.progress = batteryPct.toInt()
    }
}
