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

class PaymentAdapter(
    private val payments: List<PaymentRecord>,
    private val currencyCode: String,
    private val onEditClicked: (PaymentRecord) -> Unit,
    private val onDeleteClicked: (PaymentRecord) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.text_payment_date)
        val amountText: TextView = view.findViewById(R.id.text_payment_amount)
        val lineTop: View = view.findViewById(R.id.timeline_line_top)
        val lineBottom: View = view.findViewById(R.id.timeline_line_bottom)
        val btnDelete: MaterialButton = view.findViewById(R.id.btn_delete_payment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment_timeline, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val payment = payments[position]
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        holder.dateText.text = payment.date?.let { dateFormat.format(it) } ?: "Unknown Date"
        
        val symbol = try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
        holder.amountText.text = String.format("%s %.2f", symbol, payment.amount)

        // Timeline logic
        holder.lineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        holder.lineBottom.visibility = if (position == payments.size - 1) View.INVISIBLE else View.VISIBLE

        // Make entire row clickable for edit
        holder.itemView.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.VIRTUAL_KEY)
            onEditClicked(payment)
        }

        holder.btnDelete.setOnClickListener {
            PreferenceHelper.performHaptics(it, HapticFeedbackConstants.LONG_PRESS)
            onDeleteClicked(payment)
        }
    }

    override fun getItemCount() = payments.size
}