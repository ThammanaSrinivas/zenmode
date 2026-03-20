package com.zenlauncher.zenmode

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PageAdapter(
    private val pages: List<List<AppInfo>>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val recyclerView: RecyclerView = view.findViewById(R.id.page_grid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_page, parent, false)
        // Ensure the page takes full width/height of the parent (Outer RecyclerView)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val appsInPage = pages[position]
        
        // Setup inner 3x3 grid
        holder.recyclerView.layoutManager = GridLayoutManager(holder.itemView.context, 3)
        // Disable nested scrolling to avoid conflicts
        holder.recyclerView.isNestedScrollingEnabled = false
        
        val adapter = AppAdapter(appsInPage, onAppClick)
        holder.recyclerView.adapter = adapter
    }

    override fun getItemCount(): Int = pages.size
}
