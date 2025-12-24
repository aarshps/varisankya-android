package com.hora.varisankya

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.search.SearchBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class SubscriptionAdapter(
    private val subscriptions: List<Subscription>,
    private val onSubscriptionClicked: (Subscription) -> Unit,
    private val onSearchBarCreated: ((SearchBar) -> Unit)? = null,
    private val showSearchHeader: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val searchBar: SearchBar = view.findViewById(R.id.search_bar)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.subscription_name)
        val daysLeftTextView: TextView = view.findViewById(R.id.subscription_days_left)
        val detailsTextView: TextView = view.findViewById(R.id.subscription_details)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.subscription_progress)
        val pillContainer: MaterialCardView = view.findViewById(R.id.unified_status_pill)
    }

    override fun getItemViewType(position: Int): Int {
        return if (showSearchHeader && position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_subscription, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_HEADER) {
            val headerHolder = holder as HeaderViewHolder
            onSearchBarCreated?.invoke(headerHolder.searchBar)
            return
        }

        val itemHolder = holder as ItemViewHolder
        val actualPos = if (showSearchHeader) position - 1 else position
        val subscription = subscriptions[actualPos]
        
        itemHolder.nameTextView.text = subscription.name
        
        subscription.dueDate?.let { dueDate ->
            val format = SimpleDateFormat("MMM dd", Locale.getDefault())
            itemHolder.detailsTextView.text = "Due ${format.format(dueDate)} • ${subscription.recurrence}"

            // Days left logic
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val dueCal = Calendar.getInstance()
            dueCal.time = dueDate
            dueCal.set(Calendar.HOUR_OF_DAY, 0)
            dueCal.set(Calendar.MINUTE, 0)
            dueCal.set(Calendar.SECOND, 0)
            dueCal.set(Calendar.MILLISECOND, 0)

            val diff = dueCal.timeInMillis - today.timeInMillis
            val daysLeft = TimeUnit.MILLISECONDS.toDays(diff).toInt()

            val text = when {
                daysLeft < 0 -> "${-daysLeft}d Overdue"
                daysLeft == 0 -> "Today"
                daysLeft == 1 -> "Tomorrow"
                else -> "$daysLeft Days"
            }

            itemHolder.daysLeftTextView.text = text
            itemHolder.pillContainer.visibility = View.VISIBLE

            // Progress Bar Logic (Only 5 or fewer days left)
            val progress = when {
                daysLeft < 0 -> 100
                daysLeft <= 5 -> ((5 - daysLeft).toDouble() / 5.0 * 100).toInt()
                else -> 0
            }
            
            itemHolder.progressBar.setProgress(progress, true)
            
            // M3E Dynamic Styling
            val context = itemHolder.itemView.context
            val secondary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondary, Color.GRAY)
            val secondaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.LTGRAY)
            val onSecondaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.BLACK)
            
            when {
                daysLeft < 0 -> {
                    val errorContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorErrorContainer, Color.RED)
                    val onErrorContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnErrorContainer, Color.BLACK)
                    val errorColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorError, Color.RED)
                    
                    itemHolder.pillContainer.setCardBackgroundColor(ColorStateList.valueOf(errorContainer))
                    itemHolder.daysLeftTextView.setTextColor(onErrorContainer)
                    itemHolder.progressBar.setIndicatorColor(errorColor)
                    itemHolder.progressBar.trackColor = MaterialColors.compositeARGBWithAlpha(errorColor, 32)
                }
                daysLeft == 0 -> {
                    itemHolder.pillContainer.setCardBackgroundColor(ColorStateList.valueOf(secondary))
                    itemHolder.daysLeftTextView.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondary, Color.BLACK))
                    itemHolder.progressBar.setIndicatorColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondary, Color.WHITE))
                    itemHolder.progressBar.trackColor = MaterialColors.compositeARGBWithAlpha(Color.WHITE, 64)
                }
                else -> {
                    itemHolder.pillContainer.setCardBackgroundColor(ColorStateList.valueOf(secondaryContainer))
                    itemHolder.daysLeftTextView.setTextColor(onSecondaryContainer)
                    itemHolder.progressBar.setIndicatorColor(secondary)
                    itemHolder.progressBar.trackColor = MaterialColors.compositeARGBWithAlpha(secondary, 32)
                }
            }

        } ?: run {
            itemHolder.pillContainer.visibility = View.GONE
            itemHolder.detailsTextView.text = subscription.recurrence
        }

        itemHolder.itemView.setOnClickListener {
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            PreferenceHelper.performHaptics(it, haptic)
            onSubscriptionClicked(subscription)
        }
    }

    override fun getItemCount() = if (showSearchHeader) subscriptions.size + 1 else subscriptions.size
}