package com.zenlauncher.zenmode

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView

class AppGridManager(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private val indicatorLayout: LinearLayout
) {

    private val packageManager: PackageManager = context.packageManager

    init {
        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Add scroll listener for page indicators
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val position = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        updateIndicators(position)
                    }
                }
            }
        })
    }

    private fun loadApps() {
        val apps = getInstalledApps()
        // Chunk apps into groups of 9 (3x3)
        val pages = apps.chunked(9)

        recyclerView.adapter = PageAdapter(pages) { appInfo ->
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName.toString())
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        }
        
        setupIndicators(pages.size)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val activities = packageManager.queryIntentActivities(intent, 0)

        for (resolveInfo in activities) {
            val label = resolveInfo.loadLabel(packageManager)
            val icon = resolveInfo.loadIcon(packageManager)
            val packageName = resolveInfo.activityInfo.packageName

            apps.add(AppInfo(label, packageName, icon))
        }

        return apps.sortedBy { it.label.toString() }
    }

    private fun setupIndicators(count: Int) {
        indicatorLayout.removeAllViews()
        if (count <= 1) return // No indicators needed for 1 or 0 pages

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 0, 8, 0)

        for (i in 0 until count) {
            val dot = ImageView(context)
            dot.setImageDrawable(context.getDrawable(R.drawable.indicator_dot_inactive))
            dot.layoutParams = params
            indicatorLayout.addView(dot)
        }
        
        // Set first dot active
        if (indicatorLayout.childCount > 0) {
            (indicatorLayout.getChildAt(0) as ImageView).setImageDrawable(context.getDrawable(R.drawable.indicator_dot_active))
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val dot = indicatorLayout.getChildAt(i) as ImageView
            if (i == position) {
                dot.setImageDrawable(context.getDrawable(R.drawable.indicator_dot_active))
            } else {
                dot.setImageDrawable(context.getDrawable(R.drawable.indicator_dot_inactive))
            }
        }
    }
}
