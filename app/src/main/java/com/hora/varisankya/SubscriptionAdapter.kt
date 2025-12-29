package com.hora.varisankya

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
// Explicitly using the app's R class to access all merged attributes
import com.hora.varisankya.R

class SubscriptionAdapter(
    private var subscriptions: List<Subscription>,
    private val onSubscriptionClicked: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.subscription_name)
        val daysLeftTextView: TextView = view.findViewById(R.id.subscription_days_left)
        val detailsTextView: TextView = view.findViewById(R.id.subscription_details)
        val pillContainer: MaterialCardView = view.findViewById(R.id.unified_status_pill)
        val progressView: PillProgressView = view.findViewById(R.id.pill_progress_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val subscription = subscriptions[position]
        val context = holder.itemView.context
        
        holder.nameTextView.text = subscription.name
        
        if (!subscription.active) {
            holder.itemView.alpha = 0.6f
            holder.nameTextView.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY))
            holder.detailsTextView.text = "Discontinued • ${subscription.recurrence}"
            holder.pillContainer.visibility = View.VISIBLE
            holder.daysLeftTextView.text = "Inactive"
            holder.progressView.visibility = View.GONE
            
            // Inactive style
            val surfaceContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.LTGRAY)
            val onSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY)
            
            holder.pillContainer.setCardBackgroundColor(surfaceContainer)
            holder.daysLeftTextView.setTextColor(onSurfaceVariant)
            
        } else {
            holder.itemView.alpha = 1.0f
            holder.nameTextView.setTextColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK))
            
            subscription.dueDate?.let { dueDate ->
                val format = SimpleDateFormat("MMM dd", Locale.getDefault())
                holder.detailsTextView.text = "Due ${format.format(dueDate)} • ${subscription.recurrence}"

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

                holder.daysLeftTextView.text = text
                holder.pillContainer.visibility = View.VISIBLE
                holder.progressView.visibility = View.VISIBLE // Always visible to show track
                
                // M3E Dynamic Styling & Progress Logic
                // Progress starts when notifications start (7 days out)
                val notificationWindow = 7 
                val progress = when {
                     daysLeft < 0 -> 100 // Overdue is fully urgent
                     daysLeft > notificationWindow -> 0 // More than 7 days out = 0% progress (but track visible)
                     else -> {
                         // Scale from 0% at 7 days to 100% at 0 days
                         // daysLeft is between 0 and 7 here
                         ((notificationWindow - daysLeft).toFloat() / notificationWindow * 100).toInt()
                     }
                }
                
                // Animate progress change
                val animator = ObjectAnimator.ofInt(holder.progressView, "progress", 0, progress)
                animator.duration = 600 // Smooth standard duration
                animator.interpolator = DecelerateInterpolator() // Standard easing
                animator.start()

                // Resolve Colors
                val errorContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorErrorContainer, Color.RED)
                val onErrorContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnErrorContainer, Color.WHITE)
                
                val tertiaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiaryContainer, Color.CYAN)
                val onTertiaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnTertiaryContainer, Color.BLACK)
                
                val secondaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondaryContainer, Color.LTGRAY)
                val onSecondaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSecondaryContainer, Color.BLACK)
                
                val surfaceContainerHigh = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainerHigh, Color.LTGRAY)
                val onSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
                val outlineVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY)

                // Track Color: Always OutlineVariant (or SurfaceVariant) for distinct empty state
                holder.progressView.pillBackgroundColor = outlineVariant

                // Fill Color & Text Color based on Urgency
                when {
                    daysLeft < 0 -> {
                        // Overdue: Error Container Style
                        holder.pillContainer.setCardBackgroundColor(errorContainer)
                        holder.daysLeftTextView.setTextColor(onErrorContainer)
                        
                        // Progress Fill: Error
                        holder.progressView.progressColor = MaterialColors.getColor(context, android.R.attr.colorError, Color.RED)
                    }
                    daysLeft <= 3 -> {
                        // Very close: Tertiary Container Style
                        holder.pillContainer.setCardBackgroundColor(tertiaryContainer)
                        holder.daysLeftTextView.setTextColor(onTertiaryContainer)
                        
                        // Progress Fill: Tertiary
                        holder.progressView.progressColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiary, Color.CYAN)
                    }
                    daysLeft <= 7 -> {
                         // Close: Secondary Container Style
                        holder.pillContainer.setCardBackgroundColor(secondaryContainer)
                        holder.daysLeftTextView.setTextColor(onSecondaryContainer)
                        
                        // Progress Fill: Secondary
                        holder.progressView.progressColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondary, Color.GRAY)
                    }
                    else -> {
                        // Far away: Neutral Style
                        holder.pillContainer.setCardBackgroundColor(surfaceContainerHigh)
                        holder.daysLeftTextView.setTextColor(onSurface)
                        
                        // Progress Fill: Secondary (Invisible since progress is 0, but logical fallback)
                        holder.progressView.progressColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSecondary, Color.GRAY)
                    }
                }

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
    }

    override fun getItemCount() = subscriptions.size

    fun updateData(newSubscriptions: List<Subscription>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = subscriptions.size
            override fun getNewListSize(): Int = newSubscriptions.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return subscriptions[oldItemPosition].id == newSubscriptions[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return subscriptions[oldItemPosition] == newSubscriptions[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.subscriptions = newSubscriptions
        diffResult.dispatchUpdatesTo(this)
    }
}