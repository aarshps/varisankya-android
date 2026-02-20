package com.hora.varisankya

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.shape.ShapeAppearanceModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
// Explicitly using the app's R class is not needed when in the same package
// import com.hora.varisankya.R
import com.hora.varisankya.util.ThemeHelper
import com.hora.varisankya.util.AnimationHelper

private val DATE_FORMAT = SimpleDateFormat("MMM dd", Locale.getDefault())

// Pre-calculate shapes to avoid building them on every scroll
private var singleShape: ShapeAppearanceModel? = null
private var firstShape: ShapeAppearanceModel? = null
private var middleShape: ShapeAppearanceModel? = null
private var lastShape: ShapeAppearanceModel? = null


class SubscriptionAdapter(
    private var subscriptions: List<Subscription>,
    private val onSubscriptionClicked: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.subscription_name)
        val daysLeftTextView: TextView = view.findViewById(R.id.subscription_days_left)
        val detailsTextView: TextView = view.findViewById(R.id.subscription_details)
        val pillContainer: MaterialCardView = view.findViewById(R.id.unified_status_pill)
        val amountPill: MaterialCardView = view.findViewById(R.id.amount_pill)
        val amountTextView: TextView = view.findViewById(R.id.subscription_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return ItemViewHolder(view)
    }

    // Cached resources to eliminate allocation and resolution overhead during scroll
    private val calendar = Calendar.getInstance()
    private var isColorsResolved = false
    private var colorSurfaceVariant = 0
    private var colorOnSurfaceVariant = 0
    private var colorPrimary = 0
    private var colorOnPrimary = 0
    private var colorContainerHighest = 0
    private var colorOnSurface = 0

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val subscription = subscriptions[position]
        val context = holder.itemView.context
        
        // Resolve colors once per adapter lifecycle
        if (!isColorsResolved) {
            colorSurfaceVariant = ThemeHelper.getSurfaceVariantColor(context)
            colorOnSurfaceVariant = ThemeHelper.getOnSurfaceVariantColor(context)
            colorPrimary = ThemeHelper.getPrimaryColor(context)
            colorOnPrimary = ThemeHelper.getOnPrimaryColor(context)
            colorContainerHighest = ThemeHelper.getSurfaceContainerHighestColor(context)
            colorOnSurface = ThemeHelper.getOnSurfaceColor(context)
            isColorsResolved = true
        }

        
        holder.nameTextView.text = subscription.name
        
        // Format Amount using global currency symbol
        val globalCurrency = PreferenceHelper.getCurrency(context)
        holder.amountTextView.text = CurrencyHelper.formatCurrency(context, subscription.cost, globalCurrency)
        
        // Dynamic Grouping Logic
        val cardView = holder.itemView as MaterialCardView
        val isFirst = position == 0
        val isLast = position == subscriptions.size - 1
        val isSingle = isFirst && isLast

        if (singleShape == null) {
            singleShape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_App_SingleItem, 0).build()
            firstShape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_App_FirstItem, 0).build()
            middleShape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_App_MiddleItem, 0).build()
            lastShape = ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_App_LastItem, 0).build()
        }


        val shapeModel = when {
            isSingle -> singleShape
            isFirst -> firstShape
            isLast -> lastShape
            else -> middleShape
        }



        cardView.shapeAppearanceModel = shapeModel ?: throw IllegalStateException("Shapes not initialized")

        // Margin manipulation removed to prevent layout jitter during scroll
        // Standard spacing is handled by cardVerticalMargin in XML


        if (!subscription.active) {
            holder.itemView.alpha = 0.6f
            holder.nameTextView.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY))
            val autopayLabel = if (subscription.autopay) " • Autopay" else ""
            holder.detailsTextView.text = "Discontinued • ${subscription.recurrence}$autopayLabel"
            holder.pillContainer.visibility = View.VISIBLE
            holder.daysLeftTextView.text = "Inactive"
            
            // Inactive style: SurfaceVariant
            holder.pillContainer.setCardBackgroundColor(colorSurfaceVariant)
            holder.pillContainer.strokeWidth = 0
            holder.daysLeftTextView.setTextColor(colorOnSurfaceVariant)
            
            // Sync Amount Pill & Patch
            holder.amountPill.setCardBackgroundColor(colorSurfaceVariant)
            holder.amountPill.strokeWidth = 0
            
            // Sync Text Color
            holder.amountTextView.setTextColor(colorOnSurfaceVariant)
            
        } else {
            holder.itemView.alpha = 1.0f
            holder.nameTextView.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
            
            subscription.dueDate?.let { dueDate ->
                val autopayLabel = if (subscription.autopay) " • Autopay" else ""
                holder.detailsTextView.text = "Due ${DATE_FORMAT.format(dueDate)} • ${subscription.recurrence}$autopayLabel"

                // Days left logic without creating new Calendar instances
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayMillis = calendar.timeInMillis

                calendar.time = dueDate
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val dueMillis = calendar.timeInMillis

                val diff = dueMillis - todayMillis
                val daysLeft = TimeUnit.MILLISECONDS.toDays(diff).toInt()

                val text = when {
                    daysLeft < 0 -> "${-daysLeft}d Overdue"
                    daysLeft == 0 -> "Today"
                    daysLeft == 1 -> "Tomorrow"
                    else -> "$daysLeft Days"
                }

                holder.daysLeftTextView.text = text
                holder.pillContainer.visibility = View.VISIBLE

                // Urgency/Status pill uses dynamic primary (Tier 1)
                holder.pillContainer.setCardBackgroundColor(colorPrimary)
                holder.daysLeftTextView.setTextColor(colorOnPrimary)
                
                // Amount pill uses subtle tonal surface (Tier 2/3) for "Weather" look
                holder.amountPill.setCardBackgroundColor(colorContainerHighest)
                holder.amountTextView.setTextColor(colorOnSurface)

            } ?: run {
                holder.pillContainer.visibility = View.GONE
                holder.detailsTextView.text = subscription.recurrence
            }
        }

        holder.itemView.setOnClickListener {
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            PreferenceHelper.performHaptics(it, haptic)
            onSubscriptionClicked(subscription)
        }
        // Apply Spring Feel
    }

    override fun getItemCount() = subscriptions.size

    fun updateData(newSubscriptions: List<Subscription>) {
        val diffCallback = SubscriptionDiffCallback(this.subscriptions, newSubscriptions)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        this.subscriptions = newSubscriptions
        diffResult.dispatchUpdatesTo(this)
    }

    class SubscriptionDiffCallback(
        private val oldList: List<Subscription>,
        private val newList: List<Subscription>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Assuming Subscription has a unique ID, otherwise fallback to name+cost+dueDate
            // Using logic based on available fields since ID might not be exposed or reliable in this context if not stable
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            // Ideally compare by ID if available. Let's assume user defined ID or unique fields.
            // If explicit ID exists, use it. Based on Subscription class usage elsewhere (Firestore), it likely has an ID.
            // But I don't see the Subscription class definition here. I'll rely on object reference or content for now if ID is missing.
            // SAFEST: Compare critical business keys
            return oldItem === newItem || (oldItem.name == newItem.name && oldItem.cost == newItem.cost) 
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    private fun formatCost(amount: Double): String {
        if (amount == 0.0) return "0"
        
        return when {
            amount >= 1_000_000 -> formatValue(amount, 1_000_000.0, "m")
            amount >= 100_000 -> formatValue(amount, 100_000.0, "l")
            else -> {
                // Everything else uses 'k'
                // Constraint: Max 1 decimal, Min 0.1k
                val valueInK = amount / 1000.0
                // We want to format to 1 decimal first to check the rounded value
                val formatted = String.format(Locale.US, "%.1f", valueInK)
                
                // If the rounded value is less than 0.1 (e.g. 0.0), force it to 0.1
                // Also explicitly handle small non-zero values that round down
                if (formatted.toDouble() < 0.1) {
                    "0.1k"
                } else {
                    // Re-use logic to strip .0 if needed (e.g. 1.0k -> 1k)
                    // But wait, user said "0.5k", "0.1k".
                    // They didn't explicitly say "strip .0" in this turn, 
                    // but "1 decimal max" supports it. 
                    // Use a modified formatValue logic inline or call it.
                    val cleaned = if (formatted.endsWith(".0")) {
                        formatted.substring(0, formatted.length - 2)
                    } else {
                        formatted
                    }
                    "${cleaned}k"
                }
            }
        }
    }

    private fun formatValue(amount: Double, divisor: Double, suffix: String): String {
        val value = amount / divisor
        val formatted = String.format(Locale.US, "%.1f", value)
        return if (formatted.endsWith(".0")) {
            formatted.substring(0, formatted.length - 2) + suffix
        } else {
            formatted + suffix
        }
    }

    fun getItem(position: Int): Subscription? {
        if (position in subscriptions.indices) {
            return subscriptions[position]
        }
        return null
    }
}
