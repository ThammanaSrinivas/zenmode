package com.zenlauncher.zenmode

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zenlauncher.zenmode.coreapi.DailyUsage
import com.zenlauncher.zenmode.coreapi.services.ServiceLocator

class StatsWidgetFragment : Fragment(R.layout.layout_widget_stats) {

    private lateinit var viewModel: MainViewModel

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_IS_MY_STATS = "is_my_stats"

        fun newInstance(title: String, isMyStats: Boolean): StatsWidgetFragment {
            val fragment = StatsWidgetFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putBoolean(ARG_IS_MY_STATS, isMyStats)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Connect to Activity's ViewModel
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        val title = arguments?.getString(ARG_TITLE) ?: "Stats"
        val isMyStats = arguments?.getBoolean(ARG_IS_MY_STATS) ?: true

        val txtTitle = view.findViewById<TextView>(R.id.txt_title)
        txtTitle.text = title

        // Find the ImageView for the face
        val faceView = view.findViewById<ImageView>(R.id.img_face)

        if (!isMyStats) {
            // Observe buddy stats
            viewModel.buddyStats.observe(viewLifecycleOwner) { stats ->
                updateBuddyStatsUI(stats)
            }
        } else {
             // Observe stats only for My Stats
             viewModel.stats.observe(viewLifecycleOwner) { usage ->
                 updateMyStatsUI(usage)
             }
        }
    }

    override fun onResume() {
        super.onResume()
        // ViewModel handles data refresh on Resume via Activity or we can trigger it
        viewModel.refreshStats()
    }


    private fun updateMyStatsUI(usage: DailyUsage) {
        val view = view ?: return
        
        val txtUnlocks = view.findViewById<TextView>(R.id.txt_unlocks)
        val txtScreenTime = view.findViewById<TextView>(R.id.txt_screen_time)
        val faceView = view.findViewById<ImageView>(R.id.img_face)
        val rootView = view as? android.view.View
        val context = context ?: return

        val unlocks = usage.unlocks
        val totalMillis = usage.screenTimeInMillis
        val minutes = (totalMillis / 1000) / 60

        txtUnlocks?.text = unlocks.toString()
        txtScreenTime?.text = minutes.toString()

        // Mindfulness Logic
        val mindfulnessProgress = AppLogic.getMindfulnessPercentage(minutes)
        val mindfulnessColorRes = AppLogic.getMindfulnessColor(minutes)
        val mindfulnessColor = androidx.core.content.ContextCompat.getColor(context, mindfulnessColorRes)
        
        val segmentedBar = view.findViewById<SegmentedProgressBar>(R.id.progress_mindfulness)
        segmentedBar?.setProgress(mindfulnessProgress)
        segmentedBar?.setFilledColor(mindfulnessColor)

        // Dynamic UI for My Stats
        when (AppLogic.getMoodState(minutes)) {
            MoodState.HAPPY -> {
                rootView?.setBackgroundResource(R.drawable.bg_rounded_green_transparent)
                faceView?.setImageResource(R.drawable.face_happy)
            }
            MoodState.NEUTRAL -> {
                rootView?.setBackgroundResource(R.drawable.bg_rounded_yellow_transparent)
                faceView?.setImageResource(R.drawable.face_neutral)
            }
            MoodState.ANNOYED -> {
                rootView?.setBackgroundResource(R.drawable.bg_rounded_red_transparent)
                faceView?.setImageResource(R.drawable.face_annoyed)
            }
        }

        // Analytics
        ServiceLocator.analyticsTracker.trackDailySummaryViewed(
            dayStreak = 0, // Placeholder as streak logic isn't visible here yet, or I can fetch if available
            totalScreenTimeMin = minutes.toInt(),
            mindfulUnlockRate = if (AppConstants.GOAL_UNLOCKS_COUNT > 0) unlocks.toFloat() / AppConstants.GOAL_UNLOCKS_COUNT else 0f
        )
    }

    private fun updateBuddyStatsUI(stats: BuddyStats) {
        val view = view ?: return
        val context = context ?: return

        val txtUnlocks = view.findViewById<TextView>(R.id.txt_unlocks)
        val txtScreenTime = view.findViewById<TextView>(R.id.txt_screen_time)
        val faceView = view.findViewById<ImageView>(R.id.img_face)
        val rootView = view as? android.view.View

        val minutes = stats.screenTimeMins

        txtUnlocks?.text = stats.unlocks.toString()
        txtScreenTime?.text = minutes.toString()

        // Mindfulness Logic (same as personal stats)
        val mindfulnessProgress = AppLogic.getMindfulnessPercentage(minutes)
        val mindfulnessColorRes = AppLogic.getMindfulnessColor(minutes)
        val mindfulnessColor = androidx.core.content.ContextCompat.getColor(context, mindfulnessColorRes)

        val segmentedBar = view.findViewById<SegmentedProgressBar>(R.id.progress_mindfulness)
        segmentedBar?.setProgress(mindfulnessProgress)
        segmentedBar?.setFilledColor(mindfulnessColor)

        // Dynamic UI based on buddy stats
        when (AppLogic.getMoodState(minutes)) {
            MoodState.HAPPY -> {
                rootView?.setBackgroundResource(R.drawable.bg_rounded_green_transparent)
                faceView?.setImageResource(R.drawable.face_happy)
            }
            MoodState.NEUTRAL -> {
                rootView?.setBackgroundResource(R.drawable.bg_rounded_yellow_transparent)
                faceView?.setImageResource(R.drawable.face_neutral)
            }
            MoodState.ANNOYED -> {
                rootView?.setBackgroundResource(R.drawable.bg_rounded_red_transparent)
                faceView?.setImageResource(R.drawable.face_annoyed)
            }
        }
    }
}
