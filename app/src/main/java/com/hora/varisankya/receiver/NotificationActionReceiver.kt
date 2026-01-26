package com.hora.varisankya.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hora.varisankya.PaymentRecord
import com.hora.varisankya.Subscription
import com.hora.varisankya.util.DateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_PAID) {
            val subId = intent.getStringExtra(EXTRA_SUB_ID) ?: return
            val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
            
            // Go Async for network operations
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            
            scope.launch {
                try {
                    markAsPaid(context, subId, notifId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun markAsPaid(context: Context, subId: String, notifId: Int) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        // 1. Fetch Subscription
        val subRef = firestore.collection("users").document(userId).collection("subscriptions").document(subId)
        
        subRef.get().addOnSuccessListener { snapshot ->
            val subscription = snapshot.toObject(Subscription::class.java) ?: return@addOnSuccessListener
            
            // 2. Prepare Payment Record
            val payment = PaymentRecord(
                date = Date(),
                amount = subscription.cost,
                subscriptionName = subscription.name,
                subscriptionId = subId,
                currency = subscription.currency,
                userId = userId
            )
            
            // 3. Calculate Next Date
            val nextDueDate = subscription.recurrence.let { recurrence ->
                subscription.dueDate?.let { currentDue ->
                     DateHelper.calculateNextDueDate(currentDue, recurrence)
                }
            }

            // 4. Batch Write
            val batch = firestore.batch()
            val paymentRef = subRef.collection("payments").document()
            
            batch.set(paymentRef, payment)
            if (nextDueDate != null) {
                batch.update(subRef, "dueDate", nextDueDate)
            } else {
                // If custom/no recurrence, maybe mark inactive? leaving as is for now.
            }
            
            batch.commit().addOnSuccessListener {
                // 5. Cancel Notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notifId)
            }
        }
    }

    companion object {
        const val ACTION_MARK_PAID = "com.hora.varisankya.ACTION_MARK_PAID"
        const val EXTRA_SUB_ID = "extra_sub_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}
