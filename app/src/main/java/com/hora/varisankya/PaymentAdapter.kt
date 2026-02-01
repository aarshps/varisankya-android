package com.hora.varisankya

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class PaymentAdapter(
    private val defaultCurrency: String,
    private val onEditClicked: ((PaymentRecord) -> Unit)? = null,
    private val onDeleteClicked: ((PaymentRecord) -> Unit)? = null
) : ListAdapter<PaymentRecord, PaymentAdapter.ViewHolder>(PaymentDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayNumberText: TextView = view.findViewById(R.id.text_day_number)
        val dateText: TextView = view.findViewById(R.id.text_payment_date)
        val subNameText: TextView = view.findViewById(R.id.text_subscription_name)
        val amountPill: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.amount_pill)
        val amountText: TextView = view.findViewById(R.id.text_payment_amount)
        val btnDelete: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_delete_payment)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val payment = getItem(position)
        
        // Date formatting
        val fullFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        
        holder.dayNumberText.text = payment.date?.let { dayFormat.format(it) } ?: "?"
        holder.dateText.text = payment.date?.let { fullFormat.format(it) } ?: "Unknown Date"
        
        holder.subNameText.text = payment.subscriptionName
        
        // M3 High-Contrast Highlights: Dynamic Primary
        val context = holder.itemView.context
        
        // Use global currency
        val globalCurrency = PreferenceHelper.getCurrency(context)
        holder.amountText.text = CurrencyHelper.formatCurrency(context, payment.amount, globalCurrency)

        val primary = com.hora.varisankya.util.ThemeHelper.getPrimaryColor(context)
        val onPrimary = com.hora.varisankya.util.ThemeHelper.getOnPrimaryColor(context)
        
        holder.amountPill.setCardBackgroundColor(primary)
        holder.amountText.setTextColor(onPrimary)


        if (onEditClicked != null) {
            holder.itemView.setOnClickListener {
                PreferenceHelper.performHaptics(it, HapticFeedbackConstants.VIRTUAL_KEY)
                onEditClicked.invoke(payment)
            }
            com.hora.varisankya.util.AnimationHelper.applySpringOnTouch(holder.itemView)
        } else {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        }
        
        // Entrance
        // Entrance animation removed to fix chart sync issues and match main page scroll feel

        if (onDeleteClicked != null) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                PreferenceHelper.performHaptics(it, HapticFeedbackConstants.LONG_PRESS)
                onDeleteClicked.invoke(payment)
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    class PaymentDiffCallback : DiffUtil.ItemCallback<PaymentRecord>() {
        override fun areItemsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean {
            // Use ID if available, otherwise assume unique object ref or criteria
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean {
            return oldItem == newItem
        }
    }
}