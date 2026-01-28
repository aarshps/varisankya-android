package com.hora.varisankya.util

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.hora.varisankya.ConfirmationBottomSheet
import com.hora.varisankya.Subscription
import com.hora.varisankya.SubscriptionAdapter
import com.hora.varisankya.viewmodel.MainViewModel
import com.hora.varisankya.PreferenceHelper

class SubscriptionActionHelper(
    private val activity: FragmentActivity,
    private val viewModel: MainViewModel
) {

    fun setupSwipeActions(recyclerView: RecyclerView, adapter: SubscriptionAdapter, contentRoot: android.view.View) {
        val swipeCallback = object : SwipeActionCallback(activity) {
            override fun onSwipeRight(position: Int) {
                adapter.notifyItemChanged(position) // Reset view so it doesn't stay swiped out
                val subscription = adapter.getItem(position) ?: return
                confirmMarkAsPaid(subscription, contentRoot)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun confirmMarkAsPaid(subscription: Subscription, contentRoot: android.view.View) {
        val sheet = ConfirmationBottomSheet(
            title = "Mark as Paid",
            message = "Mark ${subscription.name} as paid for this cycle? This will update the due date.",
            positiveButtonText = "Mark Paid",
            onConfirm = {
                viewModel.markAsPaid(subscription,
                    onSuccess = {
                        PreferenceHelper.performSuccessHaptic(contentRoot)
                    },
                    onError = {
                        Toast.makeText(activity, "Failed to update", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
        sheet.show(activity.supportFragmentManager, "ConfirmPaid")
    }

    private fun confirmMarkInactive(subscription: Subscription, contentRoot: android.view.View) {
        val sheet = ConfirmationBottomSheet(
            title = "Pause Subscription",
            message = "Are you sure you want to mark ${subscription.name} as inactive? You can reactivate it later.",
            positiveButtonText = "Pause",
            onConfirm = {
                viewModel.updateSubscriptionStatus(subscription, false) {
                     PreferenceHelper.performSuccessHaptic(contentRoot)
                }
            }
        )
        sheet.show(activity.supportFragmentManager, "ConfirmInactive")
    }

    private fun confirmDeleteSubscription(subscription: Subscription, contentRoot: android.view.View) {
        val sheet = ConfirmationBottomSheet(
            title = "Delete Subscription",
            message = "Are you sure you want to delete ${subscription.name}? This action cannot be undone and will remove all payment history.",
            positiveButtonText = "Delete",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteSubscription(subscription) {
                    PreferenceHelper.performSuccessHaptic(contentRoot)
                }
            }
        )
        sheet.show(activity.supportFragmentManager, "ConfirmDelete")
    }
}
