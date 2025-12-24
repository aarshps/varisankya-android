package com.hora.varisankya

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ThemeAdapter : RecyclerView.Adapter<ThemeAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(View(parent.context))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
    override fun getItemCount() = 0
}