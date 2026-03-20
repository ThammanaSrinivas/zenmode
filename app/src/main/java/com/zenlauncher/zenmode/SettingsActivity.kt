package com.zenlauncher.zenmode

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupUI()
    }

    private fun setupUI() {
        // Back Button
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        // Back to Home Button
        findViewById<ImageView>(R.id.btn_back_home).setOnClickListener {
            finish()
        }

        // Share Launcher Button
        findViewById<ImageView>(R.id.btn_share_launcher).setOnClickListener {
            shareLauncher()
        }

        // Mission Text Styling
        val missionText = "Our mission to create a Million Focused one\nwith zen mode on. Help us build Zenmode,\ntalk to the builders Srinivas & Kamal."
        val spannable = SpannableString(missionText)

        // Bold "create a Million Focused one"
        val boldStart1 = missionText.indexOf("create a Million Focused one")
        if (boldStart1 != -1) {
            spannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                boldStart1,
                boldStart1 + "create a Million Focused one".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Bold "with zen mode on"
        val boldStart2 = missionText.indexOf("with zen mode on")
        if (boldStart2 != -1) {
            spannable.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                boldStart2,
                boldStart2 + "with zen mode on".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Green "Srinivas"
        val greenColor = Color.parseColor("#12B117") // zen_mindfulness_happy
        val srinivasStart = missionText.indexOf("Srinivas")
        if (srinivasStart != -1) {
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                srinivasStart,
                srinivasStart + "Srinivas".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Green "Kamal"
        val kamalStart = missionText.indexOf("Kamal")
        if (kamalStart != -1) {
            spannable.setSpan(
                ForegroundColorSpan(greenColor),
                kamalStart,
                kamalStart + "Kamal".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        findViewById<TextView>(R.id.tv_mission).text = spannable
    }

    private fun shareLauncher() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ZenMode Launcher")
            putExtra(Intent.EXTRA_TEXT, "Check out ZenMode, a mindful minimalist launcher: https://example.com") // Replace with actual link if known
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
}
