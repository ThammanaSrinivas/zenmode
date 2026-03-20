package com.zenlauncher.zenmode

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val apps: List<AppInfo>,
    private val onItemClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.icon_view)
        val labelView: TextView = view.findViewById(R.id.label_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_icon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.iconView.setImageDrawable(app.icon)

//        // app icons beige theme
//        // Apply Sepia/Beige Filter
//        val matrix = android.graphics.ColorMatrix()
//        matrix.setSaturation(0f) // Desaturate
//
//        // Scale RGB to be slightly Beige (R=1, G=0.95, B=0.82)
//        val sepiaMatrix = android.graphics.ColorMatrix()
//        sepiaMatrix.setScale(1.0f, 0.95f, 0.82f, 1.0f)
//
//        matrix.postConcat(sepiaMatrix)
//
//        holder.iconView.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        
        holder.labelView.text = app.label
        
        holder.itemView.setOnClickListener {
            onItemClick(app)
        }
    }

    override fun getItemCount() = apps.size
}
