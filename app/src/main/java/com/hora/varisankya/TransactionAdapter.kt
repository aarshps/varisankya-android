package com.hora.varisankya

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private val payments: List<PaymentRecord>
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount() = 0
}